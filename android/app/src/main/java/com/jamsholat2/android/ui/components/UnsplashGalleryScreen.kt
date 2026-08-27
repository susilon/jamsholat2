package com.jamsholat2.android.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jamsholat2.android.util.ImageDownloadHelper
import com.jamsholat2.android.util.UnsplashPhoto
import com.jamsholat2.android.util.UnsplashClient
import com.jamsholat2.android.util.UnsplashWebFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PRESET_QUERIES = listOf(
    "Masjid", "Kabaa", "Mecca", "Mosque", "Calligraphy", "Qur'an", "Senja", "Langit Malam"
)

private val AquaAccent = Color(0xFF00BCD4)

/**
 * Native TV-style Unsplash gallery. Queries Unsplash's public website JSON search,
 * renders a D-pad navigable grid, and saves the picked photo (high-res) into the
 * public Downloads folder via DownloadManager.
 */
@Composable
fun UnsplashGalleryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Hidden WebView used as the network layer: it solves Unsplash's anti-bot JS
    // challenge and hands us the raw JSON. See UnsplashWebFetcher.
    var fetcher by remember { mutableStateOf<UnsplashWebFetcher?>(null) }
    var didInitialLoad by remember { mutableStateOf(false) }

    var searchText by remember { mutableStateOf("Masjid") }
    var appliedQuery by remember { mutableStateOf("Masjid") }
    var photos by remember { mutableStateOf<List<UnsplashPhoto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPhoto by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var awaitingPermissionPhoto by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var loadErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Image bytes fetched through the hidden WebView (bypasses the platform network
    // stack that fails against images.unsplash.com on some TV boxes)
    val imageBytes = remember { mutableStateMapOf<String, ByteArray>() }
    val pendingByteRequests = remember { mutableSetOf<String>() }

    fun recordLoadError(id: String, message: String) {
        loadErrors = loadErrors + (id to message)
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun hasStorageWritePermission(): Boolean {
        return Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requestImageBytes(photo: UnsplashPhoto) {
        val f = fetcher ?: return
        val id = photo.id
        if (imageBytes.containsKey(id) || pendingByteRequests.contains(id)) return
        val url = photo.urls.small ?: photo.urls.thumb ?: photo.urls.regular ?: return
        pendingByteRequests.add(id)
        f.fetchBytes(url) { bytes, error ->
            pendingByteRequests.remove(id)
            if (bytes != null) {
                imageBytes[id] = bytes
            } else {
                recordLoadError(id, error ?: "gagal memuat gambar")
            }
        }
    }

    fun downloadPhoto(photo: UnsplashPhoto) {
        val rawUrl = photo.urls.raw ?: photo.urls.full ?: photo.urls.regular
        if (rawUrl.isNullOrBlank()) {
            showToast("URL gambar tidak tersedia")
            return
        }

        fun proceed() {
            val f = fetcher
            if (f == null) {
                showToast("WebView belum siap")
                return
            }
            showToast("Menyiapkan gambar...")
            val highResUrl = ImageDownloadHelper.upgradeToHighRes(rawUrl)
            f.fetchBytes(highResUrl, timeoutMs = 90_000L) { bytes, error ->
                if (bytes == null) {
                    showToast("Unduhan gagal: $error")
                    return@fetchBytes
                }
                scope.launch(Dispatchers.IO) {
                    val (ok, message) = ImageDownloadHelper.saveImageBytes(context, bytes)
                    withContext(Dispatchers.Main) {
                        if (ok) showToast("Tersimpan di folder Download: $message")
                        else showToast("Gagal menyimpan: $message")
                    }
                }
            }
        }

        if (hasStorageWritePermission()) {
            proceed()
        } else {
            awaitingPermissionPhoto = photo
        }
    }

    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val photo = awaitingPermissionPhoto
        awaitingPermissionPhoto = null
        if (granted && photo != null) {
            downloadPhoto(photo)
        } else if (!granted) {
            showToast("Izin penyimpanan ditolak")
        }
    }

    fun runSearch(newQuery: String) {
        val f = fetcher ?: return
        val q = newQuery.trim().ifEmpty { "Masjid" }
        searchText = q
        appliedQuery = q
        isLoading = true
        error = null
        f.fetch(UnsplashClient.buildSearchUrl(q, 1)) { body ->
            isLoading = false
            val resp = UnsplashClient.parseSearchResponse(body)
            if (resp == null) {
                error = "Gagal memuat gambar"
                photos = emptyList()
                page = 1
                totalPages = 1
            } else {
                photos = resp.results
                totalPages = resp.totalPages.coerceAtLeast(1)
                page = 1
                if (resp.results.isEmpty()) error = "Tidak ada hasil untuk \"$q\""
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || isLoading || page >= totalPages) return
        val f = fetcher ?: return
        val next = page + 1
        isLoadingMore = true
        f.fetch(UnsplashClient.buildSearchUrl(appliedQuery, next)) { body ->
            isLoadingMore = false
            val resp = UnsplashClient.parseSearchResponse(body)
            if (resp != null && resp.results.isNotEmpty()) {
                photos = photos + resp.results
                page = next
                totalPages = resp.totalPages.coerceAtLeast(1)
            }
        }
    }

    LaunchedEffect(fetcher) {
        if (fetcher != null && !didInitialLoad) {
            didInitialLoad = true
            runSearch(appliedQuery)
        }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIdx ->
                if (
                    lastIdx != null &&
                    photos.isNotEmpty() &&
                    !isLoading &&
                    !isLoadingMore &&
                    page < totalPages &&
                    lastIdx >= photos.size - 9
                ) {
                    loadMore()
                }
            }
    }

    // Close save dialog first, then leave the gallery.
    BackHandler(enabled = true) {
        if (selectedPhoto != null) selectedPhoto = null else onClose()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val closeInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.aquaFocusBorder(),
                    interactionSource = closeInteraction
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Unduh Gambar dari Unsplash", color = Color.White, fontSize = 15.sp)
                    Text(
                        "Foto gratis • lisensi Unsplash • tersimpan di folder Download",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f).height(52.dp),
                    placeholder = { Text("Cari gambar...", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch(searchText) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AquaAccent,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = AquaAccent
                    )
                )
                Spacer(Modifier.width(8.dp))
                val searchInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { runSearch(searchText) },
                    modifier = Modifier.aquaFocusBorder(interactionSource = searchInteraction),
                    interactionSource = searchInteraction,
                    colors = aquaButtonColors(searchInteraction)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cari", fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
                val reloadInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { runSearch(appliedQuery) },
                    modifier = Modifier.aquaFocusBorder(),
                    interactionSource = reloadInteraction
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat ulang", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESET_QUERIES.forEach { preset ->
                    val chipInteraction = remember(preset) { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { runSearch(preset) },
                        modifier = Modifier.aquaFocusBorder(
                            chipInteraction,
                            RoundedCornerShape(6.dp)
                        ),
                        interactionSource = chipInteraction,
                        colors = aquaOutlinedButtonColors(chipInteraction),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Text(preset, fontSize = 11.sp)
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && photos.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = AquaAccent)
                            Spacer(Modifier.height(10.dp))
                            Text("Mencari gambar...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                    error != null && photos.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(error ?: "Terjadi kesalahan", color = Color.White, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("Periksa koneksi internet masjid", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { runSearch(appliedQuery) }) { Text("Coba Lagi") }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(photos) { photo ->
                                GalleryCard(
                                    photo = photo,
                                    bytes = imageBytes[photo.id],
                                    onError = { p, msg ->
                                        recordLoadError(p.id, msg)
                                        // Fallback: fetch bytes via the WebView network stack
                                        requestImageBytes(p)
                                    },
                                    onClick = { selectedPhoto = photo }
                                )
                            }
                            if (photos.isNotEmpty() && page < totalPages) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = AquaAccent,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        } else {
                                            val moreInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = { loadMore() },
                                                modifier = Modifier.aquaFocusBorder(
                                                    moreInteraction,
                                                    RoundedCornerShape(6.dp)
                                                ),
                                                interactionSource = moreInteraction,
                                                colors = aquaOutlinedButtonColors(moreInteraction)
                                            ) {
                                                Text("Muat Lebih Banyak", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hidden 1px WebView: solves Unsplash's anti-bot JS challenge offscreen and
        // feeds raw JSON to the native grid above.
        AndroidView(
            factory = { ctx ->
                WebView(ctx).also { webView ->
                    fetcher = UnsplashWebFetcher(webView)
                }
            },
            modifier = Modifier
                .size(UnsplashWebFetcher.HOST_VIEW_SIZE_DP.dp)
                .alpha(0f),
            onRelease = { webView ->
                fetcher?.destroy()
                fetcher = null
                webView.destroy()
            }
        )
    }

    selectedPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPhoto = null },
            title = { Text("Simpan gambar ini?", fontSize = 17.sp) },
            text = {
                Column {
                    AsyncImage(
                        model = imageBytes[photo.id] ?: (photo.urls.regular ?: photo.urls.small),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOfNotNull(
                            photo.user?.name?.let { "Foto oleh $it" },
                            "${photo.width} x ${photo.height}"
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90CAF9)
                    )
                    Spacer(Modifier.height(6.dp))
                    loadErrors[photo.id]?.let { err ->
                        Text(
                            "Thumbnail gagal dimuat: $err",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE57373)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        "URL: ${(photo.urls.raw ?: photo.urls.regular ?: "").take(100)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Disimpan ke folder Download resolusi tinggi (1920px), lalu pilih via tombol \"Gambar\" di pengaturan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedPhoto = null
                    downloadPhoto(photo)
                }) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = AquaAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPhoto = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun GalleryCard(
    photo: UnsplashPhoto,
    bytes: ByteArray?,
    onError: (UnsplashPhoto, String) -> Unit,
    onClick: () -> Unit
) {
    val interaction = remember(photo.id) { MutableInteractionSource() }
    val context = LocalContext.current
    var failed by remember(photo.id) { mutableStateOf(false) }
    // Grid thumbnails use the small variant - fast decode, low memory on TV GPUs
    val urlRequest = remember(photo.id) {
        ImageRequest.Builder(context)
            .data(photo.urls.small ?: photo.urls.thumb ?: photo.urls.regular)
            .crossfade(150)
            .build()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF37474F))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .aquaFocusBorder(interaction, RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = bytes ?: urlRequest,
            contentDescription = photo.altDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { failed = false },
            onError = { state ->
                if (!failed) {
                    failed = true
                    onError(photo, state.result.throwable.message ?: "gagal memuat gambar")
                }
            }
        )
        if (failed && bytes == null) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Gagal dimuat",
                tint = Color(0xFFE57373),
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }
    }
}

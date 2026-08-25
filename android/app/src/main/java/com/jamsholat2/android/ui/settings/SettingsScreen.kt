package com.jamsholat2.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import com.jamsholat2.android.ui.components.aquaFocusBorder
import com.jamsholat2.android.ui.components.aquaButtonColors
import com.jamsholat2.android.ui.components.aquaOutlinedButtonColors
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import com.google.android.gms.location.LocationServices
import com.jamsholat2.android.data.AppConfig
import com.jamsholat2.android.data.BackgroundItem
import com.jamsholat2.android.util.BackgroundFileManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: AppConfig,
    onBack: () -> Unit,
    onUpdate: (AppConfig) -> Unit,
    onUseLocation: (Double, Double) -> Unit = { _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val dummyFocusRequester = remember { FocusRequester() }
    val firstGroupFocusRequester = remember { FocusRequester() }
    var showFocusBorder by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationStatus by remember { mutableStateOf("") }
    var backgroundStatus by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            scope.launch(Dispatchers.IO) {
                val item = BackgroundFileManager.copyUriToInternal(context, uri)
                withContext(Dispatchers.Main) {
                    if (item != null) {
                        val current = config.effectiveBackgroundItems().toMutableList()
                        current.add(item)
                        onUpdate(config.copy(backgroundItems = current, videolist = current.map { it.name }))
                        backgroundStatus = "Ditambahkan: ${item.name} (${item.type})"
                    } else {
                        backgroundStatus = "Gagal menyalin file"
                    }
                }
            }
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            scope.launch(Dispatchers.IO) {
                val item = BackgroundFileManager.copyUriToInternal(context, uri)
                withContext(Dispatchers.Main) {
                    if (item != null) {
                        val current = config.effectiveBackgroundItems().toMutableList()
                        current.add(item)
                        onUpdate(config.copy(backgroundItems = current, videolist = current.map { it.name }))
                        backgroundStatus = "Ditambahkan: ${item.name} (${item.type})"
                    } else {
                        backgroundStatus = "Gagal menyalin file"
                    }
                }
            }
        }
    }
    val anyPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            scope.launch(Dispatchers.IO) {
                val item = BackgroundFileManager.copyUriToInternal(context, uri)
                withContext(Dispatchers.Main) {
                    if (item != null) {
                        val current = config.effectiveBackgroundItems().toMutableList()
                        current.add(item)
                        onUpdate(config.copy(backgroundItems = current, videolist = current.map { it.name }))
                        backgroundStatus = "Ditambahkan: ${item.name} (${item.type})"
                    } else {
                        backgroundStatus = "Gagal menyalin file"
                    }
                }
            }
        }
    }
    var showFilePicker by remember { mutableStateOf(false) }
    var filePickerType by remember { mutableStateOf("all") }
    var externalMedia by remember { mutableStateOf<List<BackgroundFileManager.ExternalMedia>>(emptyList()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        // after permission, refresh list
        if (showFilePicker) {
            externalMedia = BackgroundFileManager.listExternalMedia(context, filePickerType)
            if (externalMedia.isEmpty()) backgroundStatus = "Tidak ada file ditemukan atau izin ditolak"
            else backgroundStatus = "Ditemukan ${externalMedia.size} file"
        }
    }
    fun openFilePicker(type: String) {
        filePickerType = type
        externalMedia = BackgroundFileManager.listExternalMedia(context, type)
        if (externalMedia.isEmpty()) {
            // Try requesting permissions for external storage
            val perms = mutableListOf<String>()
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                perms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                perms.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(perms.toTypedArray())
            // Still show picker with empty list to allow retry, or show status
            backgroundStatus = "Mencari file di /sdcard/Download, Movies, Pictures... ${externalMedia.size} ditemukan"
        } else {
            backgroundStatus = "Ditemukan ${externalMedia.size} file"
        }
        showFilePicker = true
    }
    var selectedPrayer by remember { mutableStateOf<String?>(null) }
    var selectedInfoIdx by remember { mutableStateOf<Int?>(null) }
    val backButtonFocusRequester = remember { FocusRequester() }
    var backButtonFocused by remember { mutableStateOf(false) }

    fun navigateBack() {
        when {
            selectedPrayer != null -> selectedPrayer = null
            selectedInfoIdx != null -> selectedInfoIdx = null
            selectedGroup != null -> selectedGroup = null
            else -> onBack()
        }
        backButtonFocused = false
    }

    // Handle back for settings: file picker handled by dialog's onDismissRequest
    BackHandler(enabled = !showFilePicker) {
        navigateBack()
    }

    LaunchedEffect(Unit) {
        try { dummyFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    LaunchedEffect(selectedGroup) {
        selectedPrayer = null
        selectedInfoIdx = null
        backButtonFocused = false
        delay(200)
        try {
            if (selectedGroup == null) firstGroupFocusRequester.requestFocus()
            else dummyFocusRequester.requestFocus()
        } catch (_: Exception) {}
        showFocusBorder = true
    }
    LaunchedEffect(selectedPrayer, selectedInfoIdx) {
        if (selectedPrayer != null || selectedInfoIdx != null) {
            backButtonFocused = false
            delay(150)
            try { dummyFocusRequester.requestFocus() } catch (_: Exception) {}
            showFocusBorder = true
        }
    }

    val titleMap = mapOf(
        "identitas" to "Identitas Masjid",
        "umum" to "Umum",
        "waktu" to "Waktu Sholat",
        "teks" to "Teks Berjalan",
        "info" to "Info",
        "video" to "Gambar Latar"
    )

    // TV-style: right-side panel with dimmed background, like Android TV settings
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onBack() }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key != Key.Back) {
                    showFocusBorder = true
                }
                false
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.42f)
                .background(Color(0xFF1E1E1E))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
        ) {
            Scaffold(
                topBar = {
                    val currentTitle = when {
                        selectedPrayer != null -> selectedPrayer ?: "Waktu Sholat"
                        selectedInfoIdx != null -> if (selectedInfoIdx == 7) "Info Khotbah Jumat" else "Edit Info ${selectedInfoIdx!! + 1}"
                        else -> titleMap[selectedGroup] ?: "Settings"
                    }
                    TopAppBar(
                        title = { Text(currentTitle, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(
                                onClick = { navigateBack() },
                                modifier = Modifier
                                    .focusRequester(backButtonFocusRequester)
                                    .focusable()
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = if (backButtonFocused) Color(0xFF00BCD4) else Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF2D2D2D),
                            titleContentColor = Color.White
                        )
                    )
                },
                containerColor = Color(0xFF1E1E1E),
                modifier = Modifier.fillMaxSize()
            ) { padding ->
                // Hidden dummy for focus clearing
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(dummyFocusRequester)
                        .focusable()
                )

                if (selectedGroup == null) {
                    // LEVEL 1: Group list only – Android TV style
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .imePadding(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "Pilih kategori untuk mengatur",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        val groups = listOf(
                            Triple("Identitas Masjid", "Nama & alamat masjid", "identitas"),
                            Triple("Umum", "Locale, mazhab, lokasi, beep", "umum"),
                            Triple("Waktu Sholat", "Label, iqomah, adjustment, durasi", "waktu"),
                            Triple("Teks Berjalan", "Scrolling text & speed", "teks"),
                            Triple("Info", "Info cards & jadwal Jumat", "info"),
                            Triple("Gambar Latar", "Background image & video list", "video")
                        )
                        items(groups.size) { idx ->
                            val (title, subtitle, key) = groups[idx]
                            val interaction = remember(key) { MutableInteractionSource() }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aquaFocusBorder(interaction, RoundedCornerShape(10.dp), enabled = showFocusBorder),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (idx == 0) Modifier.focusRequester(firstGroupFocusRequester) else Modifier)
                                        .clickable(interactionSource = interaction, indication = null) { selectedGroup = key }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                        Spacer(Modifier.height(2.dp))
                                        Text(subtitle, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(0.6f))
                                }
                            }
                        }
                    }
                } else {
                    // LEVEL 2: Single group detail – only that group's settings
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .imePadding(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedGroup) {
                            "identitas" -> item {
                                SettingsSection(title = "Identitas Masjid") {
                                    OutlinedTextField(
                                        value = config.namamasjid,
                                        onValueChange = { onUpdate(config.copy(namamasjid = it)) },
                                        label = { Text("Nama Masjid") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = config.alamatmasjid,
                                        onValueChange = { onUpdate(config.copy(alamatmasjid = it)) },
                                        label = { Text("Alamat Masjid") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                            "umum" -> item {
                                SettingsSection(title = "Umum") {
                                    OutlinedTextField(
                                        value = config.locale,
                                        onValueChange = { onUpdate(config.copy(locale = it)) },
                                        label = { Text("Locale (id, en)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    var madhabExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = madhabExpanded, onExpandedChange = { madhabExpanded = !madhabExpanded }) {
                                        OutlinedTextField(
                                            value = if (config.madhab == "hanafi") "Hanafi" else "Syafii",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Mazhab") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(madhabExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = madhabExpanded, onDismissRequest = { madhabExpanded = false }) {
                                            DropdownMenuItem(text = { Text("Syafii") }, onClick = { onUpdate(config.copy(madhab = "syafii")); madhabExpanded = false })
                                            DropdownMenuItem(text = { Text("Hanafi") }, onClick = { onUpdate(config.copy(madhab = "hanafi")); madhabExpanded = false })
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    var calcExpanded by remember { mutableStateOf(false) }
                                    val methods = listOf("Egyptian","Muslim World League","Karachi","UmmAlQura","Singapore","North America","Dubai","Qatar","Kuwait","MoonsightingCommittee")
                                    ExposedDropdownMenuBox(expanded = calcExpanded, onExpandedChange = { calcExpanded = !calcExpanded }) {
                                        OutlinedTextField(
                                            value = methods.getOrNull(config.calculation) ?: methods[0],
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Metode") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(calcExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = calcExpanded, onDismissRequest = { calcExpanded = false }) {
                                            methods.forEachIndexed { idx, name ->
                                                DropdownMenuItem(text = { Text(name) }, onClick = { onUpdate(config.copy(calculation = idx)); calcExpanded = false })
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = config.latlngdata,
                                        onValueChange = { onUpdate(config.copy(latlngdata = it)) },
                                        label = { Text("Lat, Long") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                scope.launch {
                                                    try {
                                                        val fused = LocationServices.getFusedLocationProviderClient(context)
                                                        fused.lastLocation.addOnSuccessListener { loc ->
                                                            if (loc != null) {
                                                                onUpdate(config.copy(latlngdata = "${loc.latitude}, ${loc.longitude}"))
                                                                onUseLocation(loc.latitude, loc.longitude)
                                                                locationStatus = "Location updated"
                                                            } else locationStatus = "No location available"
                                                        }.addOnFailureListener { locationStatus = "Failed: ${it.message}" }
                                                    } catch (e: Exception) { locationStatus = "Error: ${e.message}" }
                                                }
                                            }) { Icon(Icons.Default.MyLocation, contentDescription = "Use location") }
                                        }
                                    )
                                    if (locationStatus.isNotEmpty()) Text(locationStatus, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = config.fajrAngle.toString(),
                                            onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(config.copy(fajrAngle = it)) } },
                                            label = { Text("Sudut Subuh") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        OutlinedTextField(
                                            value = config.ishaAngle.toString(),
                                            onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(config.copy(ishaAngle = it)) } },
                                            label = { Text("Sudut Isya") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = config.imsak.toString(),
                                        onValueChange = { v -> v.toIntOrNull()?.let { onUpdate(config.copy(imsak = it)) } },
                                        label = { Text("Imsak (menit sebelum subuh)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                     Spacer(Modifier.height(8.dp))
                                     Text(
                                         "Beep Volume: ${(config.beep.beepVolume*100).toInt()}%",
                                         color = Color.White,
                                         fontSize = 14.sp,
                                         fontWeight = FontWeight.Medium
                                     )
                                     val beepInteraction = remember { MutableInteractionSource() }
                                     Slider(
                                         value = config.beep.beepVolume,
                                         onValueChange = { onUpdate(config.copy(beep = config.beep.copy(beepVolume = it))) },
                                         valueRange = 0f..1f,
                                         modifier = Modifier
                                             .aquaFocusBorder(beepInteraction, RoundedCornerShape(8.dp), enabled = showFocusBorder)
                                             .focusable()
                                             .onPreviewKeyEvent { event ->
                                                 if (event.type == KeyEventType.KeyDown) {
                                                     when (event.key) {
                                                         Key.DirectionRight -> {
                                                             val newVal = (config.beep.beepVolume + 0.05f).coerceAtMost(1f)
                                                             onUpdate(config.copy(beep = config.beep.copy(beepVolume = newVal)))
                                                             true
                                                         }
                                                         Key.DirectionLeft -> {
                                                             val newVal = (config.beep.beepVolume - 0.05f).coerceAtLeast(0f)
                                                             onUpdate(config.copy(beep = config.beep.copy(beepVolume = newVal)))
                                                             true
                                                         }
                                                         else -> false
                                                     }
                                                                  } else false
                                                              }
                                                              )
                                    Text("Jam komputer GMT: ${java.util.TimeZone.getDefault().rawOffset/3600000}", style = MaterialTheme.typography.bodySmall)
                                    Text("Ref: https://github.com/batoulapps/adhan-js/blob/master/METHODS.md", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            "waktu" -> item {
                                if (selectedPrayer == null) {
                                    SettingsSection(title = "Waktu Sholat") {
                                        Text("Pilih waktu untuk mengedit", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(Modifier.height(8.dp))
                                        config.prayer.entries.forEach { (key, cfg) ->
                                            val interaction = remember(key) { MutableInteractionSource() }
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).aquaFocusBorder(interaction, RoundedCornerShape(10.dp), enabled = showFocusBorder),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = interaction, indication = null) { selectedPrayer = key }.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(key, color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                                                        Spacer(Modifier.height(2.dp))
                                                        val durText = if (key == "Dzuhur") "Dur: ${cfg.duration}m (Jumat: ${cfg.durationJumat}m)" else "Dur: ${cfg.duration}m"
                                                        Text("Label: ${cfg.label} • Iqomah: ${cfg.iqomah}m • Adj: ${cfg.adjustment}m • $durText", color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                                                    }
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(0.6f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val key = selectedPrayer!!
                                    val cfg = config.prayer[key] ?: config.prayer.values.first()
                                    val isTerbit = key == "Terbit"
                                    SettingsSection(title = "Edit $key") {
                                        OutlinedTextField(
                                            value = cfg.label,
                                            onValueChange = { newVal ->
                                                val newMap = config.prayer.toMutableMap()
                                                newMap[key] = cfg.copy(label = newVal)
                                                onUpdate(config.copy(prayer = newMap))
                                            },
                                            label = { Text("Label $key") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        if (!isTerbit) {
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = cfg.iqomah.toString(),
                                                onValueChange = { v -> v.toIntOrNull()?.let { nv ->
                                                    val newMap = config.prayer.toMutableMap()
                                                    newMap[key] = cfg.copy(iqomah = nv)
                                                    onUpdate(config.copy(prayer = newMap))
                                                }},
                                                label = { Text("Waktu Iqomah (menit)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = cfg.adjustment.toString(),
                                                onValueChange = { v -> v.toIntOrNull()?.let { nv ->
                                                    val newMap = config.prayer.toMutableMap()
                                                    newMap[key] = cfg.copy(adjustment = nv)
                                                    onUpdate(config.copy(prayer = newMap))
                                                }},
                                                label = { Text("Adjustment") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                            OutlinedTextField(
                                                value = cfg.duration.toString(),
                                                onValueChange = { v -> v.toIntOrNull()?.let { nv ->
                                                    val newMap = config.prayer.toMutableMap()
                                                    newMap[key] = cfg.copy(duration = nv)
                                                    onUpdate(config.copy(prayer = newMap))
                                                }},
                                                label = { Text("Durasi (menit)") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        if (key == "Dzuhur") {
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = cfg.durationJumat.toString(),
                                                onValueChange = { v -> v.toIntOrNull()?.let { nv ->
                                                    val newMap = config.prayer.toMutableMap()
                                                    newMap[key] = cfg.copy(durationJumat = nv)
                                                    onUpdate(config.copy(prayer = newMap))
                                                }},
                                                label = { Text("Durasi (Solat Jumat) (menit)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                            Text("Digunakan saat Dzuhur di hari Jumat", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            "teks" -> item {
                                SettingsSection(title = "Teks Berjalan") {
                                    OutlinedTextField(
                                        value = config.scrollingdata.value,
                                        onValueChange = { onUpdate(config.copy(scrollingdata = config.scrollingdata.copy(value = it))) },
                                        label = { Text("Scrolling Text") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = config.scrollingdata.valueOnPray,
                                        onValueChange = { onUpdate(config.copy(scrollingdata = config.scrollingdata.copy(valueOnPray = it))) },
                                        label = { Text("Scrolling Text Saat Sholat") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                    Spacer(Modifier.height(8.dp))
                                      Text(
                                          "Scrolling Speed: ${config.scrollingdata.speed}",
                                          color = Color.White,
                                          fontSize = 14.sp,
                                          fontWeight = FontWeight.Medium
                                      )
                                      val speedInteraction = remember { MutableInteractionSource() }
                                      Slider(
                                          value = config.scrollingdata.speed.toFloat(),
                                          onValueChange = { onUpdate(config.copy(scrollingdata = config.scrollingdata.copy(speed = it.toInt()))) },
                                          valueRange = 1f..9f,
                                          steps = 7,
                                          modifier = Modifier
                                              .aquaFocusBorder(speedInteraction, RoundedCornerShape(8.dp), enabled = showFocusBorder)
                                              .focusable()
                                              .onPreviewKeyEvent { event ->
                                                  if (event.type == KeyEventType.KeyDown) {
                                                      when (event.key) {
                                                          Key.DirectionRight -> {
                                                              val newVal = (config.scrollingdata.speed + 1).coerceAtMost(9)
                                                              onUpdate(config.copy(scrollingdata = config.scrollingdata.copy(speed = newVal)))
                                                              true
                                                          }
                                                          Key.DirectionLeft -> {
                                                              val newVal = (config.scrollingdata.speed - 1).coerceAtLeast(1)
                                                              onUpdate(config.copy(scrollingdata = config.scrollingdata.copy(speed = newVal)))
                                                              true
                                                          }
                                                          else -> false
                                                      }
                                                  } else false
                                              }
                                      )
                                }
                            }
                            "info" -> item {
                                if (selectedInfoIdx == null) {
                                    SettingsSection(title = "Info") {
                                        Text("Pilih info untuk mengedit", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Spacer(Modifier.height(8.dp))
                                        config.infotextdata.forEachIndexed { idx, item ->
                                            val interaction = remember(idx) { MutableInteractionSource() }
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).aquaFocusBorder(interaction, RoundedCornerShape(10.dp), enabled = showFocusBorder),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable(interactionSource = interaction, indication = null) { selectedInfoIdx = idx }.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(if (idx == 7) "Info Khusus Saat Khotbah Jum'at" else item.title.ifBlank { "Slot kosong ${idx+1}" }, color = Color.White, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1)
                                                        Spacer(Modifier.height(2.dp))
                                                        val badge = if (idx == 7) "(hanya muncul saat dzuhur di hari jumat)" else if (item.enable) "Active • ${item.duration} detik" else "Not Active • ${item.duration} detik"
                                                        Text(badge, color = if (item.enable) Color(0xFF81C784) else Color.Gray, fontSize = 11.sp, maxLines = 1)
                                                        if (item.content.isNotBlank()) {
                                                            Text(item.content.take(60).replace("\n", " "), color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                                                        }
                                                    }
                                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(0.6f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val idx = selectedInfoIdx!!
                                    val item = config.infotextdata.getOrNull(idx) ?: config.infotextdata.first()
                                    SettingsSection(title = if (idx == 7) "Info Khotbah Jumat" else "Edit Info ${idx+1}") {
                                        if (idx == 7) {
                                            Text("Info Khusus Saat Khotbah Jum'at", style = MaterialTheme.typography.titleSmall)
                                            Spacer(Modifier.height(8.dp))
                                        } else {
                                            OutlinedTextField(
                                                value = item.title,
                                                onValueChange = {
                                                    val list = config.infotextdata.toMutableList()
                                                    list[idx] = item.copy(title = it)
                                                    onUpdate(config.copy(infotextdata = list))
                                                },
                                                label = { Text("Judul Info") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = item.duration.toString(),
                                                    onValueChange = { v ->
                                                        v.toIntOrNull()?.let { d ->
                                                            val list = config.infotextdata.toMutableList()
                                                            list[idx] = list[idx].copy(duration = d)
                                                            onUpdate(config.copy(infotextdata = list))
                                                        }
                                                    },
                                                    label = { Text("Durasi (detik)") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Text("Active", modifier = Modifier.weight(1f))
                                                    Switch(
                                                        checked = item.enable,
                                                        onCheckedChange = {
                                                            val list = config.infotextdata.toMutableList()
                                                            list[idx] = item.copy(enable = it)
                                                            onUpdate(config.copy(infotextdata = list))
                                                        }
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }
                                        OutlinedTextField(
                                            value = item.content,
                                            onValueChange = {
                                                val list = config.infotextdata.toMutableList()
                                                list[idx] = list[idx].copy(content = it)
                                                onUpdate(config.copy(infotextdata = list))
                                            },
                                            label = { Text("Content (HTML allowed)") },
                                            modifier = Modifier.fillMaxWidth().height(140.dp),
                                            minLines = 4
                                        )
                                        if (idx == 7) Text("(hanya muncul saat dzuhur di hari jumat)", style = MaterialTheme.typography.bodySmall)
                                        else {
                                            Spacer(Modifier.height(4.dp))
                                            Text(if (item.enable) "Active" else "Not Active", style = MaterialTheme.typography.bodySmall, color = if (item.enable) Color(0xFF81C784) else Color.Gray)
                                        }
                                    }
                                }
                            }
                            "video" -> item {
                                SettingsSection(title = "Latar Belakang") {
                                    Text("Video & Gambar Latar", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Pilih video/gambar dari penyimpanan. File akan dicopy ke folder internal (videos/images) dan ditampilkan bergantian. Video sesuai durasi, gambar 10 detik. Saat waktu sholat latar menjadi hitam.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    val bgItems = config.effectiveBackgroundItems()
                                    if (bgItems.isEmpty()) {
                                        Text("Belum ada latar. Default: tawaf.mp4", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    } else {
                                        bgItems.forEachIndexed { idx, item ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
                                            ) {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        if (item.type == "video") Icons.Default.VideoLibrary else Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = if (item.type == "video") Color(0xFF81C784) else Color(0xFF90CAF9)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(item.name, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                                        Text(
                                                            if (item.type == "video") "Video • durasi sesuai file" else "Gambar • 10 detik",
                                                            color = Color.Gray, fontSize = 10.sp
                                                        )
                                                    }
                                                    run {
                                                        val deleteInteraction = remember(idx) { MutableInteractionSource() }
                                                        val isFocused by deleteInteraction.collectIsFocusedAsState()
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch(Dispatchers.IO) {
                                                                    BackgroundFileManager.deleteItem(context, item)
                                                                    withContext(Dispatchers.Main) {
                                                                        val newList = bgItems.filterIndexed { i, _ -> i != idx }
                                                                        val toSave = if (newList.isEmpty()) listOf(BackgroundItem("tawaf.mp4", "video")) else newList
                                                                        onUpdate(config.copy(backgroundItems = toSave, videolist = toSave.map { it.name }))
                                                                        backgroundStatus = "Dihapus: ${item.name}"
                                                                    }
                                                                }
                                                            },
                                                            interactionSource = deleteInteraction
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Hapus",
                                                                tint = if (isFocused) Color(0xFF00BCD4) else Color(0xFFE57373)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (backgroundStatus.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(backgroundStatus, style = MaterialTheme.typography.bodySmall, color = Color(0xFF81C784))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Tambah dari penyimpanan:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val videoInteraction = remember { MutableInteractionSource() }
                                        val imageInteraction = remember { MutableInteractionSource() }
                                        Button(
                                            onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                            modifier = Modifier.weight(1f),
                                            interactionSource = videoInteraction,
                                            colors = aquaButtonColors(videoInteraction)
                                        ) {
                                            Icon(Icons.Default.VideoLibrary, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Video", fontSize = 13.sp)
                                        }
                                        Button(
                                            onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                            modifier = Modifier.weight(1f),
                                            interactionSource = imageInteraction,
                                            colors = aquaButtonColors(imageInteraction)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Gambar", fontSize = 13.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    run {
                                        val allInteraction = remember { MutableInteractionSource() }
                                        OutlinedButton(
                                            onClick = { anyPickerLauncher.launch(arrayOf("video/*", "image/*")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            interactionSource = allInteraction,
                                            colors = aquaOutlinedButtonColors(allInteraction)
                                        ) {
                                            Text("Pilih Video atau Gambar (Semua)", fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Atau browse langsung (TV):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        run {
                                            val vInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = { openFilePicker("video") },
                                                modifier = Modifier.weight(1f),
                                                interactionSource = vInteraction,
                                                colors = aquaOutlinedButtonColors(vInteraction)
                                            ) {
                                                Text("Browse Video File", fontSize = 11.sp)
                                            }
                                        }
                                        run {
                                            val iInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = { openFilePicker("image") },
                                                modifier = Modifier.weight(1f),
                                                interactionSource = iInteraction,
                                                colors = aquaOutlinedButtonColors(iInteraction)
                                            ) {
                                                Text("Browse Gambar File", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    run {
                                        val all2Interaction = remember { MutableInteractionSource() }
                                        OutlinedButton(
                                            onClick = { openFilePicker("all") },
                                            modifier = Modifier.fillMaxWidth(),
                                            interactionSource = all2Interaction,
                                            colors = aquaOutlinedButtonColors(all2Interaction)
                                        ) {
                                            Text("Browse Semua File", fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Catatan:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF90CAF9))
                                    Text("• File dicopy ke ${context.filesDir}/videos atau /images", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                                    Text("• Latar berganti otomatis sesuai list", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                                    Text("• Saat sholat (isPraying) latar hitam", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
        if (showFilePicker) {
            AlertDialog(
                onDismissRequest = { showFilePicker = false },
                title = { Text("Pilih File - ${filePickerType.uppercase()}") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        Text("File di Download/Movies/Pictures:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        if (externalMedia.isEmpty()) {
                            Text("Tidak ada file ${filePickerType} ditemukan di penyimpanan.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            Text("Pastikan file ada di /sdcard/Download dan izin diberikan.", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                            Spacer(Modifier.height(6.dp))
                            Button(onClick = {
                                externalMedia = BackgroundFileManager.listExternalMedia(context, filePickerType)
                                backgroundStatus = "Scan: ${externalMedia.size} file ditemukan"
                            }) { Text("Refresh") }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(externalMedia.size) { idx ->
                                    val m = externalMedia[idx]
                                    val isVideo = m.type == "video"
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                scope.launch(Dispatchers.IO) {
                                                    val item = if (m.file != null && m.file.exists()) {
                                                        BackgroundFileManager.copyFileToInternal(context, m.file)
                                                    } else {
                                                        BackgroundFileManager.copyUriToInternal(context, m.uri)
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        if (item != null) {
                                                            val current = config.effectiveBackgroundItems().toMutableList()
                                                            current.add(item)
                                                            onUpdate(config.copy(backgroundItems = current, videolist = current.map { it.name }))
                                                            backgroundStatus = "Ditambahkan dari file: ${item.name}"
                                                            showFilePicker = false
                                                        } else {
                                                            backgroundStatus = "Gagal copy ${m.name}"
                                                        }
                                                    }
                                                }
                                            }.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isVideo) Icons.Default.VideoLibrary else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (isVideo) Color(0xFF81C784) else Color(0xFF90CAF9),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(m.name, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                                val sizeInfo = m.file?.let { "${it.length() / 1024} KB" } ?: "MediaStore"
                                                Text("${if (isVideo) "Video" else "Gambar"} • $sizeInfo", color = Color.Gray, fontSize = 9.sp, maxLines = 1)
                                            }
                                            Text("Pilih", color = Color(0xFF90CAF9), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilePicker = false }) { Text("Tutup") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        externalMedia = BackgroundFileManager.listExternalMedia(context, filePickerType)
                        backgroundStatus = "Refresh: ${externalMedia.size} file"
                    }) { Text("Refresh") }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF90CAF9))
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

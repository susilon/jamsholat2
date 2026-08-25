package com.jamsholat2.android.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.jamsholat2.android.data.BackgroundItem
import com.jamsholat2.android.util.BackgroundFileManager
import kotlinx.coroutines.delay

@Composable
fun VideoBackground(
    backgroundItems: List<BackgroundItem>,
    isPraying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // When praying, show black background per requirement
    if (isPraying) {
        Box(modifier = modifier.background(Color.Black)) {}
        return
    }

    val effectiveItems = backgroundItems.ifEmpty {
        listOf(BackgroundItem("tawaf.mp4", "video"))
    }

    if (effectiveItems.isEmpty()) {
        Box(modifier = modifier.background(Color.Black)) {}
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    // Ensure index valid when list changes
    LaunchedEffect(effectiveItems.size) {
        if (currentIndex >= effectiveItems.size) currentIndex = 0
    }

    val currentItem = effectiveItems[currentIndex % effectiveItems.size]
    val uriString = remember(currentItem) {
        BackgroundFileManager.resolveUriForItem(context, currentItem)
    }

    // If uri null, skip after short delay
    if (uriString == null) {
        LaunchedEffect(currentItem) {
            delay(1000)
            currentIndex = (currentIndex + 1) % effectiveItems.size
        }
        Box(modifier = modifier.background(Color.Black)) {}
        return
    }

    if (currentItem.type == "image") {
        // Image: display for 10 seconds then next, with error handling
        LaunchedEffect(currentItem) {
            try {
                delay(10_000)
                if (effectiveItems.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % effectiveItems.size
                }
            } catch (_: Exception) {
                if (effectiveItems.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % effectiveItems.size
                }
            }
        }
        val model = try {
            when {
                uriString.startsWith("asset:///") -> {
                    // Coil asset: file:///android_asset/
                    val assetPath = uriString.removePrefix("asset:///")
                    "file:///android_asset/$assetPath"
                }
                uriString.startsWith("file://") || uriString.startsWith("/") -> uriString
                else -> uriString
            }
        } catch (_: Exception) {
            uriString
        }
        AsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = {
                // On error, will still advance via outer LaunchedEffect after 10s
            }
        )
        // Keep black background behind image while loading
        // AsyncImage will cover, no need extra
    } else {
        // Video: play and advance when ended, duration is video duration
        var exoPlayer by remember(uriString) { mutableStateOf<ExoPlayer?>(null) }
        var hasEnded by remember { mutableStateOf(false) }

        DisposableEffect(uriString) {
            hasEnded = false
            var player: ExoPlayer? = null
            var listener: Player.Listener? = null
            try {
                player = ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    volume = 0f
                }
                val mediaItem = try {
                    MediaItem.fromUri(uriString)
                } catch (_: Exception) {
                    null
                }
                listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            hasEnded = true
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        // On error, skip to next after short delay
                        hasEnded = true
                    }
                }
                player.addListener(listener)
                if (mediaItem != null) {
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                } else {
                    hasEnded = true
                }
                exoPlayer = player
            } catch (e: Exception) {
                hasEnded = true
                exoPlayer = null
            }
            onDispose {
                try {
                    if (player != null && listener != null) player.removeListener(listener)
                    player?.release()
                } catch (_: Exception) {}
                exoPlayer = null
            }
        }

        LaunchedEffect(hasEnded) {
            if (hasEnded) {
                try {
                    // small delay to avoid flicker
                    delay(300)
                    if (effectiveItems.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % effectiveItems.size
                    }
                } catch (_: Exception) {
                    if (effectiveItems.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % effectiveItems.size
                    }
                }
            }
        }

        val currentPlayer = exoPlayer
        if (currentPlayer != null) {
            AndroidView(
                modifier = modifier,
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = currentPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = false
                        isFocusableInTouchMode = false
                    }
                },
                update = { view ->
                    view.player = currentPlayer
                }
            )
        } else {
            Box(modifier = modifier.background(Color.Black))
        }
    }
}

// Legacy overload for backward compatibility with List<String> videolist
@Composable
fun VideoBackground(
    videoList: List<String>,
    modifier: Modifier = Modifier
) {
    val items = videoList.map { BackgroundItem.fromFileName(it) }
    VideoBackground(backgroundItems = items, isPraying = false, modifier = modifier)
}

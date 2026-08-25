package com.jamsholat2.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jamsholat2.android.ui.JamSholatScreen
import com.jamsholat2.android.ui.JamSholatViewModel
import com.jamsholat2.android.ui.settings.SettingsScreen
import com.jamsholat2.android.ui.theme.JamSholatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JamSholatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on (kiosk)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Fullscreen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Request location permission early
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
        }

        setContent {
            JamSholatTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showSettings by remember { mutableStateOf(false) }
                var showExitDialog by remember { mutableStateOf(false) }

                // Handle back when on main screen: show exit confirmation
                // When Settings is open, SettingsScreen handles back itself
                // When dialog is already shown, let AlertDialog's onDismissRequest handle back
                BackHandler(enabled = !showSettings && !showExitDialog) {
                    showExitDialog = true
                }

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                                    if (!showSettings) {
                                        showExitDialog = !showExitDialog
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                false
                            }
                    ) {
                        // Kiosk always visible behind panel (TV style)
                        JamSholatScreen(
                            uiState = uiState,
                            onMosqueNameClick = { showSettings = true },
                            onAddressClick = { showSettings = true },
                            onDateTimeClick = { showSettings = true },
                            onInfoClick = { showSettings = true },
                            onTimeBoxClick = { showSettings = true },
                            onMarqueeClick = { showSettings = true },
                            onSettingsClick = { showSettings = true },
                            isActive = !showSettings
                        )

                        // TV side panel – only shows at the right side, like Android TV settings
                        if (showSettings) {
                            SettingsScreen(
                                config = uiState.config,
                                onBack = { showSettings = false },
                                onUpdate = { updated -> viewModel.updateConfig { updated } },
                                onUseLocation = { lat, lng ->
                                    viewModel.updateConfig { it.copy(latlngdata = "$lat, $lng") }
                                }
                            )
                        }

                        if (showExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitDialog = false },
                                title = { Text("Konfirmasi") },
                                text = { Text("Apakah anda meu menutup aplikasi?") },
                                confirmButton = {
                                    TextButton(onClick = { finish() }) {
                                        Text("Ya")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showExitDialog = false }) {
                                        Text("Tidak")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

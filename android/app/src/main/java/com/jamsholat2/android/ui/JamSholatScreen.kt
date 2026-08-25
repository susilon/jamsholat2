package com.jamsholat2.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamsholat2.android.data.AppConfig
import com.jamsholat2.android.domain.PrayerCalculator
import com.jamsholat2.android.ui.components.*
import com.jamsholat2.android.util.DateTimeUtil
import java.util.Date

@Composable
fun JamSholatScreen(
    uiState: JamSholatUiState,
    onMosqueNameClick: () -> Unit,
    onAddressClick: () -> Unit,
    onDateTimeClick: () -> Unit,
    onInfoClick: () -> Unit,
    onTimeBoxClick: (String) -> Unit,
    onMarqueeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isActive: Boolean = true
) {
    val config = uiState.config
    val calc = uiState.calculated
    val isPraying = uiState.isPraying
    val dummyFocusRequester = remember { FocusRequester() }
    var showFocusBorder by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            try { dummyFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Back handling is done at MainActivity level to show exit confirmation dialog.
    // This screen no longer consumes back; let Activity's BackHandler show "Apakah anda meu menutup aplikasi?" dialog.

    // InteractionSources for aqua focus border on actionable elements
    val mosqueInteraction = remember { MutableInteractionSource() }
    val addressInteraction = remember { MutableInteractionSource() }
    val dateTimeInteraction = remember { MutableInteractionSource() }
    val infoInteraction = remember { MutableInteractionSource() }
    val marqueeInteraction = remember { MutableInteractionSource() }

    // Build prayer map like js/app.js _buildPrayerMap
    val prayerItems = remember(calc, config) {
        if (calc == null) {
            listOf(
                Triple("Imsak", "--:--", "Imsak"),
                Triple("Subuh", "--:--", "Subuh"),
                Triple("Terbit", "--:--", "Terbit"),
                Triple("Dzuhur", "--:--", "Dzuhur"),
                Triple("Ashar", "--:--", "Ashar"),
                Triple("Maghrib", "--:--", "Maghrib"),
                Triple("Isya", "--:--", "Isya")
            )
        } else {
            listOf(
                Triple(config.prayer["Subuh"]?.label ?: "Subuh", PrayerCalculator.formatHHmm(calc.imsak), "Imsak"),
                Triple(config.prayer["Subuh"]?.label ?: "Subuh", PrayerCalculator.formatHHmm(calc.fajr), "Subuh"),
                Triple(config.prayer["Terbit"]?.label ?: "Terbit", PrayerCalculator.formatHHmm(calc.sunrise), "Terbit"),
                Triple(config.prayer["Dzuhur"]?.label ?: "Dzuhur", PrayerCalculator.formatHHmm(calc.dhuhr), "Dzuhur"),
                Triple(config.prayer["Ashar"]?.label ?: "Ashar", PrayerCalculator.formatHHmm(calc.asr), "Ashar"),
                Triple(config.prayer["Maghrib"]?.label ?: "Maghrib", PrayerCalculator.formatHHmm(calc.maghrib), "Maghrib"),
                Triple(config.prayer["Isya"]?.label ?: "Isya", PrayerCalculator.formatHHmm(calc.isha), "Isya")
            )
            // Fix labels: we need actual per-key mapping
            listOf(
                Triple(config.prayer["Subuh"]?.label ?: "Subuh" , PrayerCalculator.formatHHmm(calc.imsak), "Imsak"),
                Triple(config.prayer["Subuh"]?.label ?: "Subuh", PrayerCalculator.formatHHmm(calc.fajr), "Subuh"),
                Triple(config.prayer["Terbit"]?.label ?: "Terbit", PrayerCalculator.formatHHmm(calc.sunrise), "Terbit"),
                Triple(config.prayer["Dzuhur"]?.label ?: "Dzuhur", PrayerCalculator.formatHHmm(calc.dhuhr), "Dzuhur"),
                Triple(config.prayer["Ashar"]?.label ?: "Ashar", PrayerCalculator.formatHHmm(calc.asr), "Ashar"),
                Triple(config.prayer["Maghrib"]?.label ?: "Maghrib", PrayerCalculator.formatHHmm(calc.maghrib), "Maghrib"),
                Triple(config.prayer["Isya"]?.label ?: "Isya", PrayerCalculator.formatHHmm(calc.isha), "Isya")
            ).mapIndexed { idx, t ->
                // Ensure label correctness: Imsak uses its own? In js, imsak label is fixed "Imsak" not from prayer map
                when (idx) {
                    0 -> Triple("Imsak", t.second, "Imsak")
                    else -> t
                }
            }
        }
    }

    // Determine current/next for styling
    val currentKey = uiState.currentPrayerKey // fajr etc
    val nextKey = uiState.nextPrayerKey

    // Calculate timeInto warning logic like js
    val now = uiState.now
    val timeIntoMinutes = remember(calc, now, nextKey) {
        if (calc == null) 999 else {
            val nextDate = when (nextKey) {
                "fajr" -> calc.fajr
                "sunrise" -> calc.sunrise
                "dhuhr" -> calc.dhuhr
                "asr" -> calc.asr
                "maghrib" -> calc.maghrib
                "isha" -> calc.isha
                else -> calc.fajr
            }
            var diff = nextDate.time - now.time
            if (diff < 0) {
                // next is tomorrow fajr
                diff += 24 * 60 * 60 * 1000L
            }
            (diff / 1000 / 60).toInt()
        }
    }

    val timePassedMinutes = remember(calc, now, currentKey) {
        if (calc == null) 999 else {
            val currentDate = when (currentKey) {
                "fajr" -> calc.fajr
                "sunrise" -> calc.sunrise
                "dhuhr" -> calc.dhuhr
                "asr" -> calc.asr
                "maghrib" -> calc.maghrib
                "isha" -> calc.isha
                else -> calc.isha
            }
            var diff = now.time - currentDate.time
            if (diff < 0) diff += 24 * 60 * 60 * 1000L
            (diff / 1000 / 60).toInt()
        }
    }

    // Stable background list to prevent recomposition resets
    val bgItems = remember(config.backgroundItems, config.videolist) { config.effectiveBackgroundItems() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(if (isActive) Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key != Key.Back) {
                    showFocusBorder = true
                }
                false
            } else Modifier)
    ) {
        // Background carousel: video/image per 10s for images, black when praying
        VideoBackground(
            backgroundItems = bgItems,
            isPraying = isPraying,
            modifier = Modifier.fillMaxSize()
        )

        // Dummy focusable to allow clearing focus via back button
        Box(
            modifier = Modifier
                .size(1.dp)
                .focusRequester(dummyFocusRequester)
                .focusable()
        )

        // Foreground container
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .aquaFocusBorder(mosqueInteraction, RoundedCornerShape(8.dp), enabled = showFocusBorder && isActive)
                        .clickable(enabled = isActive, interactionSource = mosqueInteraction, indication = null) { onMosqueNameClick() }
                ) {
                    Text(
                        text = config.namamasjid,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )
                    Text(
                        text = config.alamatmasjid,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .aquaFocusBorder(addressInteraction, RoundedCornerShape(4.dp), enabled = showFocusBorder && isActive)
                            .clickable(enabled = isActive, interactionSource = addressInteraction, indication = null) { onAddressClick() }
                    )
                }
                Spacer(modifier = Modifier.weight(0.1f))
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .aquaFocusBorder(dateTimeInteraction, RoundedCornerShape(8.dp), enabled = showFocusBorder && isActive)
                        .clickable(enabled = isActive, interactionSource = dateTimeInteraction, indication = null) { onDateTimeClick() },
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = if (uiState.showHijri) uiState.hijriText else uiState.gregorianText,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        lineHeight = 26.sp
                    )
                }
            }

            // Info card area: show countdown during iqomah, otherwise hide when praying
            val iqomahCountdown = uiState.iqomahCountdownSeconds
            val showCountdown = iqomahCountdown != null
            AnimatedVisibility(visible = !isPraying || showCountdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, end = 12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    // Need offset like margin-left 25% in original -> align right with width 50%
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .aquaFocusBorder(infoInteraction, RoundedCornerShape(8.dp), enabled = showFocusBorder && isActive)
                            .clickable(enabled = isActive, interactionSource = infoInteraction, indication = null) { onInfoClick() }
                    ) {
                        if (showCountdown) {
                            val minutes = iqomahCountdown!! / 60
                            val seconds = iqomahCountdown % 60
                            val timeText = String.format("%d:%02d", minutes, seconds)
                            val markdown = "**Iqomah**\n# $timeText"
                            MarkdownInfoCard(markdownContent = markdown, modifier = Modifier.fillMaxWidth())
                        } else {
                            InfoCard(htmlContent = uiState.infoHtml, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Prayer time boxes footer (hide-onpray)
            AnimatedVisibility(visible = !isPraying) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .height(120.dp)
                                         .padding(horizontal = 4.dp, vertical = 6.dp),
                                     horizontalArrangement = Arrangement.spacedBy(6.dp)
                                 ) {
                    prayerItems.forEach { (label, time, key) ->
                        // Map key to prayer key for current/next
                        val prayerKeyForBox = when (key) {
                            "Imsak" -> "imsak"
                            "Subuh" -> "fajr"
                            "Terbit" -> "sunrise"
                            "Dzuhur" -> "dhuhr"
                            "Ashar" -> "asr"
                            "Maghrib" -> "maghrib"
                            "Isya" -> "isha"
                            else -> ""
                        }
                        val isCurrent = currentKey == prayerKeyForBox || (key == "Imsak" && nextKey == "fajr" && timeIntoMinutes <= config.timeintowarning)
                        val isNext = nextKey == prayerKeyForBox
                        val isBlinkingGreen = isNext && timeIntoMinutes <= config.timeintowarning
                        val isBlinkingRedCurrent = isCurrent && timePassedMinutes <= config.prayertimewarning
                        // Special imsak blink when next is fajr
                        val isImsakBlink = key == "Imsak" && nextKey == "fajr" && timeIntoMinutes <= config.timeintowarning

                        if (key != "Imsak") {
                            val boxInteraction = remember(key) { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .aquaFocusBorder(boxInteraction, RoundedCornerShape(7.dp), enabled = showFocusBorder && isActive)
                                    .clickable(enabled = isActive, interactionSource = boxInteraction, indication = null) { onTimeBoxClick(key) }
                            ) {
                                TimeBox(
                                    label = label,
                                    time = time,
                                    isCurrent = isCurrent && !isNext,
                                    isNext = isNext,
                                    isBlinkingRed = isBlinkingRedCurrent || isImsakBlink,
                                    isBlinkingGreen = isBlinkingGreen,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                TimeBox(
                                    label = label,
                                    time = time,
                                    isCurrent = isCurrent && !isNext,
                                    isNext = isNext,
                                    isBlinkingRed = isBlinkingRedCurrent || isImsakBlink,
                                    isBlinkingGreen = isBlinkingGreen,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Marquee footer always visible (colorRotate)
            Box(
                modifier = Modifier
                    .aquaFocusBorder(marqueeInteraction, RoundedCornerShape(4.dp), enabled = showFocusBorder && isActive)
                    .clickable(enabled = isActive, interactionSource = marqueeInteraction, indication = null) { onMarqueeClick() }
            ) {
                MarqueeFooter(
                    text = uiState.scrollingText,
                    speed = config.scrollingdata.speed
                )
            }
        }
    }
}

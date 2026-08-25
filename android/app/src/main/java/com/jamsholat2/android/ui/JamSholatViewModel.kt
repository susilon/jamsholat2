package com.jamsholat2.android.ui

import android.app.Application
import android.icu.util.IslamicCalendar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jamsholat2.android.data.AppConfig
import com.jamsholat2.android.data.ConfigRepository
import com.jamsholat2.android.domain.BeepPlayer
import com.jamsholat2.android.domain.CalculatedPrayerTimes
import com.jamsholat2.android.domain.PrayerCalculator
import com.jamsholat2.android.util.DateTimeUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class JamSholatUiState(
    val config: AppConfig = AppConfig.default(),
    val now: Date = Date(),
    val gregorianText: String = "",
    val hijriText: String = "",
    val showHijri: Boolean = false,
    val calculated: CalculatedPrayerTimes? = null,
    val currentPrayerKey: String = "none", // fajr/sunrise/dhuhr/asr/maghrib/isha/none
    val nextPrayerKey: String = "fajr",
    val isJumat: Boolean = false,
    val isPraying: Boolean = false,
    val isIqomah: Boolean = false,
    val isAdhan: Boolean = false,
    val infoHtml: String = "",
    val infoIndex: Int = 0,
    val scrollingText: String = "",
    val isLoading: Boolean = true,
    val iqomahCountdownSeconds: Int? = null
)

class JamSholatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ConfigRepository(application.applicationContext)
    private val beepPlayer = BeepPlayer()

    private val _uiState = MutableStateFlow(JamSholatUiState())
    val uiState: StateFlow<JamSholatUiState> = _uiState

    private var tickerJob: Job? = null
    private var infoJob: Job? = null

    // runtime flags mirroring js global.isadhan etc
    private var isAdhanFlag = false
    private var isIqomahFlag = false
    private var isPrayingFlag = false
    private var infoCtr = 0

    init {
        viewModelScope.launch {
            val cfg = repo.loadConfig()
            _uiState.value = _uiState.value.copy(config = cfg, isLoading = false)
            computePrayers(cfg)
            startTicker()
            startInfoTicker(cfg)
            updateScrollingText(cfg)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val now = Date()
                val cfg = _uiState.value.config
                val greg = DateTimeUtil.formatGregorian(now, cfg.locale)
                val hijr = DateTimeUtil.formatHijri(now, cfg.locale)
                val showHijri = Calendar.getInstance().get(Calendar.SECOND) >= 30
                _uiState.value = _uiState.value.copy(
                    now = now,
                    gregorianText = greg,
                    hijriText = hijr,
                    showHijri = showHijri
                )
                computePrayers(cfg, now)
                delay(1000)
            }
        }
    }

    private fun startInfoTicker(cfg: AppConfig) {
        infoJob?.cancel()
        infoJob = viewModelScope.launch {
            while (true) {
                val currentCfg = _uiState.value.config
                val isJumat = _uiState.value.isJumat
                val currentPray = _uiState.value.currentPrayerKey
                val isPraying = _uiState.value.isPraying
                // special jumat: only when praying, zuhur, and friday (sesuai request)
                if (isPraying && isJumat && currentPray == "dhuhr") {
                    val jumatItem = currentCfg.infotextdata.getOrNull(7)
                    if (jumatItem != null && jumatItem.content.isNotBlank()) {
                        _uiState.value = _uiState.value.copy(infoHtml = jumatItem.content, infoIndex = 7)
                        // show during praying duration, check every second
                        delay(1000)
                        continue
                    }
                }
                // Find next valid info (skip disabled, empty, and special jumat index 7 on normal time)
                var nextIdx = infoCtr % maxOf(1, currentCfg.infotextdata.size)
                var found: Int? = null
                var attempts = 0
                while (attempts < currentCfg.infotextdata.size) {
                    val candidate = currentCfg.infotextdata.getOrNull(nextIdx)
                    // skip special jumat on normal time (already handled above), and blank/disabled
                    val isSpecialJumat = nextIdx == 7
                    if (candidate != null && candidate.content.isNotBlank() && candidate.enable && !isSpecialJumat) {
                        found = nextIdx
                        break
                    }
                    nextIdx = (nextIdx + 1) % currentCfg.infotextdata.size
                    attempts++
                }
                if (found != null) {
                    val item = currentCfg.infotextdata[found]
                    _uiState.value = _uiState.value.copy(infoHtml = item.content, infoIndex = found)
                    infoCtr = (found + 1) % currentCfg.infotextdata.size
                    delay(item.duration.coerceAtLeast(1) * 1000L)
                } else {
                    // No valid info, keep previous and wait
                    delay(5000)
                    infoCtr = (infoCtr + 1) % maxOf(1, currentCfg.infotextdata.size)
                }
            }
        }
    }

    private fun updateScrollingText(cfg: AppConfig) {
        val isPraying = isPrayingFlag || isIqomahFlag
        val data = cfg.scrollingdata
        val value = data.value
        val valueOnPray = data.valueOnPray
        val text = if (isPraying) valueOnPray else value
        // Build like js: '<small>jamsholat.id</small>- ' + value.split('\n').join(' -<small>jamsholat.id</small>- ') + ' -'
        val parts = text.split("\n").filter { it.isNotBlank() }
        val joined = if (parts.isEmpty()) text else parts.joinToString(" - jamsholat.id - ")
        val finalText = if (isPraying) "- $joined -" else "jamsholat.id - $joined -"
        _uiState.value = _uiState.value.copy(scrollingText = finalText)
    }

    private fun computePrayers(cfg: AppConfig, now: Date = Date()) {
        try {
            val calc = PrayerCalculator.calculate(cfg, now)
            val prayerTimes = calc.raw

            var current = PrayerCalculator.currentPrayerLabel(prayerTimes, now)
            var next = PrayerCalculator.nextPrayerLabel(prayerTimes, now)

            if (current == "none") current = "isha"
            if (next == "none") next = "fajr"

            // day name Jumat check like js/app.js:295 moment format dddd
            val dayName = DateTimeUtil.dayNameIndo(now, cfg.locale)
            val isJumat = dayName.equals("Jumat", ignoreCase = true) || dayName.equals("Friday", ignoreCase = true) || dayName.equals("Jum'at", ignoreCase = true)

            // time passed/into calculations like js/app.js:371
            val currentDate = when (current) {
                "fajr" -> prayerTimes.fajr
                "sunrise" -> prayerTimes.sunrise
                "dhuhr" -> prayerTimes.dhuhr
                "asr" -> prayerTimes.asr
                "maghrib" -> prayerTimes.maghrib
                "isha" -> prayerTimes.isha
                else -> prayerTimes.isha
            }
            val nextDate = when (next) {
                "fajr" -> prayerTimes.fajr
                "sunrise" -> prayerTimes.sunrise
                "dhuhr" -> prayerTimes.dhuhr
                "asr" -> prayerTimes.asr
                "maghrib" -> prayerTimes.maghrib
                "isha" -> prayerTimes.isha
                else -> prayerTimes.fajr
            }

            var timePassed = now.time - currentDate.time
            var timeInto = nextDate.time - now.time

            // Handle cross-day like js
            if (PrayerCalculator.currentPrayerLabel(prayerTimes, now) == "none") {
                // yesterday isha
                val cal = Calendar.getInstance()
                cal.time = prayerTimes.isha
                cal.add(Calendar.DAY_OF_YEAR, -1)
                timePassed = now.time - cal.timeInMillis
            }
            if (PrayerCalculator.nextPrayerLabel(prayerTimes, now) == "none") {
                // tomorrow fajr
                val cal = Calendar.getInstance()
                cal.time = prayerTimes.fajr
                cal.add(Calendar.DAY_OF_YEAR, 1)
                timeInto = cal.timeInMillis - now.time
            }

            val minutesInto = (timeInto / 1000 / 60).toInt()
            val minutesPassed = (timePassed / 1000 / 60).toInt()

            // State transitions mirroring js
            // Adhan at timePassed==0
            if (minutesPassed == 0 && current != "sunrise") {
                if (!isAdhanFlag) {
                    isAdhanFlag = true
                    beepPlayer.longBeep(cfg.beep.beepVolume, cfg.beep.beepFrequency)
                }
            }

            // Iqomah at iqomah minutes
            val currentLabelForMap = PrayerCalculator.mapPrayerKeyToBoxLabel(current)
            val prayerCfg = cfg.prayer[currentLabelForMap]
            val iqomahMinutes = prayerCfg?.iqomah ?: cfg.iqomahtime

            if (minutesPassed == iqomahMinutes && current != "sunrise") {
                if (!isIqomahFlag) {
                    isIqomahFlag = true
                    beepPlayer.multipleBeep(viewModelScope, cfg.beep.beepVolume, cfg.beep.beepFrequency, cfg.beep.beepDuration, cfg.beep.beepTimes)
                    updateScrollingText(cfg.copy(isadhan = isAdhanFlag, isiqomah = true, ispraying = isPrayingFlag))
                } else {
                    isAdhanFlag = false
                }
            }

            val duration = if (current == "dhuhr" && isJumat) {
                prayerCfg?.durationJumat ?: 30
            } else {
                prayerCfg?.duration ?: 0
            }
            val prayDuration = iqomahMinutes + duration

            if (minutesPassed >= iqomahMinutes && minutesPassed <= prayDuration) {
                if (current != "sunrise") {
                    if (isIqomahFlag || !isPrayingFlag) {
                        isIqomahFlag = false
                        isPrayingFlag = true
                        updateScrollingText(cfg.copy(isadhan = isAdhanFlag, isiqomah = false, ispraying = true))
                    }
                }
            }

            if (minutesPassed > prayDuration) {
                if (isPrayingFlag) {
                    isPrayingFlag = false
                    updateScrollingText(cfg.copy(isadhan = isAdhanFlag, isiqomah = isIqomahFlag, ispraying = false))
                }
            }

            // Iqomah countdown: from prayer time until iqomah (exclusive), for display in info section as markdown
            val iqomahCountdownSec: Int? = if (current != "sunrise" && current != "none" && timePassed >= 0 && timePassed < iqomahMinutes * 60000L) {
                val remainingMs = iqomahMinutes * 60000L - timePassed
                (remainingMs / 1000).toInt().coerceAtLeast(0)
            } else null

            _uiState.value = _uiState.value.copy(
                calculated = calc,
                currentPrayerKey = current,
                nextPrayerKey = next,
                isJumat = isJumat,
                isPraying = isPrayingFlag,
                isIqomah = isIqomahFlag,
                isAdhan = isAdhanFlag,
                iqomahCountdownSeconds = iqomahCountdownSec
            )

        } catch (e: Exception) {
            // ignore
        }
    }

    fun playShortBeep() {
        val cfg = _uiState.value.config
        beepPlayer.beep(cfg.beep.beepVolume, cfg.beep.beepFrequency, cfg.beep.beepDuration)
    }

    fun playDoubleBeep() {
        val cfg = _uiState.value.config
        beepPlayer.doubleBeep(cfg.beep.beepVolume, cfg.beep.beepFrequency)
    }

    suspend fun saveConfig(newConfig: AppConfig) {
        repo.saveConfig(newConfig)
        _uiState.value = _uiState.value.copy(config = newConfig)
        computePrayers(newConfig)
        updateScrollingText(newConfig)
        // restart info ticker with new config
        startInfoTicker(newConfig)
    }

    fun updateConfig(transform: (AppConfig) -> AppConfig) {
        viewModelScope.launch {
            val current = repo.loadConfig()
            val updated = transform(current)
            saveConfig(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        infoJob?.cancel()
        beepPlayer.cancel()
    }
}

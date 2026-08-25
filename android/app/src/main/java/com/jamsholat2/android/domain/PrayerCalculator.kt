package com.jamsholat2.android.domain

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.jamsholat2.android.data.AppConfig
import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

data class CalculatedPrayerTimes(
    val imsak: Date,
    val fajr: Date,
    val sunrise: Date,
    val dhuhr: Date,
    val asr: Date,
    val maghrib: Date,
    val isha: Date,
    val raw: PrayerTimes
)

object PrayerCalculator {

    fun getCalculationParameters(config: AppConfig): CalculationParameters {
        val params = when (config.calculation) {
            0 -> CalculationMethod.EGYPTIAN.parameters
            1 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            2 -> CalculationMethod.KARACHI.parameters
            3 -> CalculationMethod.UMM_AL_QURA.parameters
            4 -> CalculationMethod.SINGAPORE.parameters
            5 -> CalculationMethod.NORTH_AMERICA.parameters
            6 -> CalculationMethod.DUBAI.parameters
            7 -> CalculationMethod.QATAR.parameters
            8 -> CalculationMethod.KUWAIT.parameters
            9 -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            else -> CalculationMethod.EGYPTIAN.parameters
        }

        // Apply custom angles (mirrors params.fajrAngle = global.fajrAngle)
        // adhan 1.0.1 has fajrAngle/ishaAngle as public fields
        try {
            val fajrField = params.javaClass.getDeclaredField("fajrAngle")
            fajrField.isAccessible = true
            fajrField.setDouble(params, config.fajrAngle)
        } catch (_: Exception) { }
        try {
            val ishaField = params.javaClass.getDeclaredField("ishaAngle")
            ishaField.isAccessible = true
            ishaField.setDouble(params, config.ishaAngle)
        } catch (_: Exception) { }

        params.madhab = if (config.madhab.equals("hanafi", ignoreCase = true)) Madhab.HANAFI else Madhab.SHAFI

        // Per-prayer adjustments: params.adjustments.fajr = global.prayer.Subuh.adjustment etc
        // adhan library's adjustments map field name is 'adjustments' with PrayerAdjustments object
        try {
            val adj = params.adjustments
            adj.fajr = config.prayer["Subuh"]?.adjustment ?: 0
            adj.sunrise = config.prayer["Terbit"]?.adjustment ?: 0
            adj.dhuhr = config.prayer["Dzuhur"]?.adjustment ?: 0
            adj.asr = config.prayer["Ashar"]?.adjustment ?: 0
            adj.maghrib = config.prayer["Maghrib"]?.adjustment ?: 0
            adj.isha = config.prayer["Isya"]?.adjustment ?: 0
        } catch (_: Exception) { }

        return params
    }

    fun calculate(config: AppConfig, date: Date = Date()): CalculatedPrayerTimes {
        val coords = Coordinates(config.getLatitude(), config.getLongitude())
        val params = getCalculationParameters(config)
        val dateComponents = DateComponents.from(date)
        val prayerTimes = PrayerTimes(coords, dateComponents, params)

        // Imsak = fajr - imsak minutes (js/app.js:352)
        val imsakDate = Date(prayerTimes.fajr.time - config.imsak * 60L * 1000L)

        return CalculatedPrayerTimes(
            imsak = imsakDate,
            fajr = prayerTimes.fajr,
            sunrise = prayerTimes.sunrise,
            dhuhr = prayerTimes.dhuhr,
            asr = prayerTimes.asr,
            maghrib = prayerTimes.maghrib,
            isha = prayerTimes.isha,
            raw = prayerTimes
        )
    }

    fun formatHHmm(date: Date): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    fun currentPrayerLabel(prayerTimes: PrayerTimes, now: Date = Date()): String {
        // mirrors prayerTimes.currentPrayer(now) in JS
        return when {
            now >= prayerTimes.isha -> "isha"
            now >= prayerTimes.maghrib -> "maghrib"
            now >= prayerTimes.asr -> "asr"
            now >= prayerTimes.dhuhr -> "dhuhr"
            now >= prayerTimes.sunrise -> "sunrise"
            now >= prayerTimes.fajr -> "fajr"
            else -> "none"
        }
    }

    fun nextPrayerLabel(prayerTimes: PrayerTimes, now: Date = Date()): String {
        return when {
            now >= prayerTimes.isha -> "none"
            now >= prayerTimes.maghrib -> "isha"
            now >= prayerTimes.asr -> "maghrib"
            now >= prayerTimes.dhuhr -> "asr"
            now >= prayerTimes.sunrise -> "dhuhr"
            now >= prayerTimes.fajr -> "sunrise"
            else -> "fajr"
        }
    }

    fun mapPrayerKeyToBoxLabel(key: String): String = when (key) {
        "fajr" -> "Subuh"
        "sunrise" -> "Terbit"
        "dhuhr" -> "Dzuhur"
        "asr" -> "Ashar"
        "maghrib" -> "Maghrib"
        "isha" -> "Isya"
        "imsak" -> "Imsak"
        else -> key
    }

    fun timeForPrayer(calc: CalculatedPrayerTimes, key: String): Date = when (key) {
        "fajr", "subuh" -> calc.fajr
        "sunrise", "terbit" -> calc.sunrise
        "dhuhr", "dzuhur" -> calc.dhuhr
        "asr", "ashar" -> calc.asr
        "maghrib" -> calc.maghrib
        "isha", "isya" -> calc.isha
        "imsak" -> calc.imsak
        else -> calc.fajr
    }
}

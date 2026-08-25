package com.jamsholat2.android.util

import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import android.os.Build

object DateTimeUtil {

    fun formatGregorian(date: Date, localeStr: String): String {
        val locale = try { Locale.forLanguageTag(localeStr) } catch (_: Exception) { Locale("id") }
        val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm:ss", locale)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    fun formatHijri(date: Date, localeStr: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val ic = android.icu.util.IslamicCalendar()
                ic.time = date
                
                val hijriMonths = listOf(
                    "Muharram", "Safar", "Rabi'ul Awwal", "Rabi'ul Akhir",
                    "Jumadil Ula", "Jumadil Akhira", "Rajab", "Sha'ban",
                    "Ramadan", "Syawal", "Zulkaidah", "Zulhijjah"
                )
                
                val day = ic.get(android.icu.util.Calendar.DAY_OF_MONTH)
                val monthIdx = ic.get(android.icu.util.Calendar.MONTH)
                val year = ic.get(android.icu.util.Calendar.YEAR)
                
                val monthName = hijriMonths.getOrElse(monthIdx) { "Unknown" }
                val hijriDate = "$day $monthName $year"
                
                val userLocale = try { Locale.forLanguageTag(localeStr) } catch (_: Exception) { Locale("id") }
                val timeSdf = SimpleDateFormat("HH:mm:ss", userLocale)
                timeSdf.timeZone = TimeZone.getDefault()
                val time = timeSdf.format(date)
                
                "$hijriDate, $time"
            } else {
                formatGregorian(date, localeStr)
            }
        } catch (e: Exception) {
            formatGregorian(date, localeStr)
        }
    }

    fun getTimezoneGmtString(): String {
        val tz = TimeZone.getDefault()
        val offsetHours = tz.rawOffset / (1000 * 60 * 60)
        val sign = if (offsetHours >= 0) "+" else ""
        return "GMT$sign$offsetHours"
    }

    fun dayNameIndo(date: Date, localeStr: String): String {
        val locale = try { Locale.forLanguageTag(localeStr) } catch (_: Exception) { Locale("id") }
        val sdf = SimpleDateFormat("EEEE", locale)
        return sdf.format(date)
    }
}

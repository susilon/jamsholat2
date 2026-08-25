package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

/**
 * Mirrors buildDefaultConfig() in js/app.js:45
 * Field names kept compatible but converted to Kotlin idiomatic types where possible.
 */
@Serializable
data class AppConfig(
    val locale: String = "id",
    val calculation: Int = 0, // 0 Egyptian,1 MWL,2 Karachi,3 UmmAlQura,4 Singapore,5 NorthAmerica,6 Dubai,7 Qatar,8 Kuwait,9 MoonsightingCommittee
    val latlngdata: String = "-6.224655537226517, 106.80679437749554",
    val fajrAngle: Double = 19.5,
    val ishaAngle: Double = 17.5,
    val madhab: String = "syafii", // syafii / hanafi
    val minuteadjustment: Int = 2, // legacy global
    val imsak: Int = 10,
    val timeintowarning: Int = 10,
    val prayertimewarning: Int = 1,
    // transient runtime flags - persisted but default false
    val isadhan: Boolean = false,
    val iqomahtime: Int = 5, // legacy per-prayer now
    val isiqomah: Boolean = false,
    val prayduration: Int = 10,
    val ispraying: Boolean = false,
    val isjumat: Boolean = false,
    val currentpray: String = "",
    val namamasjid: String = "Nama Masjid",
    val alamatmasjid: String = "Alamat Lengkap dan Nomor Telepon.",
    val prayer: Map<String, PrayerConfig> = mapOf(
        "Subuh" to PrayerConfig(label = "Subuh", iqomah = 5, adjustment = 2, duration = 5),
        "Terbit" to PrayerConfig(label = "Terbit", iqomah = 5, adjustment = 2, duration = 0),
        "Dzuhur" to PrayerConfig(label = "Dzuhur", iqomah = 5, adjustment = 2, duration = 10, durationJumat = 30),
        "Ashar" to PrayerConfig(label = "Ashar", iqomah = 5, adjustment = 2, duration = 10),
        "Maghrib" to PrayerConfig(label = "Maghrib", iqomah = 5, adjustment = 2, duration = 5),
        "Isya" to PrayerConfig(label = "Isya", iqomah = 5, adjustment = 2, duration = 10)
    ),
    val infotextinterval: Int = 5,
    val infotextdata: List<InfoTextItem> = listOf(
        InfoTextItem(
            title = "Hadist Ilmu",
            content = "<span style=\"font-size: 39px;\">مَنْ سَلَكَ طَرِيْقًايَلْتَمِسُ فِيْهِ عِلْمًا,سَهَّلَ اللهُ لَهُ طَرِيْقًا إِلَى الجَنَّةِ . رَوَاهُ مُسْلِم</span><br>Barang siapa menempuh satu jalan (cara) untuk mendapatkan ilmu, maka Allah pasti mudahkan baginya jalan menuju surga.\" (HR. Muslim)",
            enable = true,
            duration = 10
        ),
        InfoTextItem(
            title = "Iklan Jam Sholat",
            content = "Jam Sholat<br>Yuk kita bikin petunjuk waktu sholat dengan mudah.<br>jamsholat.susilon.com",
            enable = true,
            duration = 5
        ),
        InfoTextItem(title = "Slot kosong", content = "", enable = false, duration = 5),
        InfoTextItem(title = "Slot kosong", content = "", enable = false, duration = 5),
        InfoTextItem(title = "Slot kosong", content = "", enable = false, duration = 5),
        InfoTextItem(title = "Slot kosong", content = "", enable = false, duration = 5),
        InfoTextItem(title = "Slot kosong", content = "", enable = false, duration = 5),
        InfoTextItem(
            title = "Info Khusus Saat Khotbah Jumat",
            content = "<span style=\"font-size:39px\">إذا قلت لصاحبك يوم الجمعة أنصت والإمام يخطب فقد لغوت</span><br>Jika engkau berkata kepada temanmu pada hari jum’at, ‘diam dan perhatikanlah’, sedangkan imam sedang berkhutbah, maka engkau telah berbuat sia-sia.” (HR. Al-Bukhari [934].",
            enable = false,
            duration = 0
        )
    ),
    val scrollingdata: ScrollingData = ScrollingData(),
    val beep: BeepConfig = BeepConfig(),
    val videolist: List<String> = listOf("tawaf.mp4"),
    val backgroundItems: List<BackgroundItem> = emptyList()
) {
    companion object {
        fun default(): AppConfig = AppConfig(
            backgroundItems = listOf(BackgroundItem("tawaf.mp4", "video"))
        )
    }

    fun getLatitude(): Double = latlngdata.split(",").getOrNull(0)?.trim()?.toDoubleOrNull() ?: -6.224655537226517
    fun getLongitude(): Double = latlngdata.split(",").getOrNull(1)?.trim()?.toDoubleOrNull() ?: 106.80679437749554

    fun copyWithLatLng(lat: Double, lng: Double): AppConfig = copy(latlngdata = "$lat, $lng")

    fun effectiveBackgroundItems(): List<BackgroundItem> {
        return if (backgroundItems.isNotEmpty()) backgroundItems
        else if (videolist.isNotEmpty()) videolist.map { BackgroundItem.fromFileName(it) }
        else listOf(BackgroundItem("tawaf.mp4", "video"))
    }
}

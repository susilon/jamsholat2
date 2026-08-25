package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

@Serializable
data class PrayerConfig(
    val label: String = "",
    val iqomah: Int = 5,
    val adjustment: Int = 2,
    val duration: Int = 5,
    val durationJumat: Int = 30
)

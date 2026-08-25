package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

@Serializable
data class BeepConfig(
    val beepTimes: Int = 5,
    val beepVolume: Float = 0.5f,
    val beepFrequency: Int = 4000,
    val beepType: String = "square",
    val beepDuration: Int = 150
)

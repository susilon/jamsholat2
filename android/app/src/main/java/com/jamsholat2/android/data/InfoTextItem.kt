package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

@Serializable
data class InfoTextItem(
    val title: String = "Slot kosong",
    val content: String = "",
    val enable: Boolean = false,
    // duration stored as String or Int in JS; we normalize to Int seconds
    val duration: Int = 5
)

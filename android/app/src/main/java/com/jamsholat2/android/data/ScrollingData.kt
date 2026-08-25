package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

@Serializable
data class ScrollingData(
    val value: String = "Scrolling Text, Klik disini untuk mengganti text, dan juga klik di area yang akan diedit.",
    val valueOnPray: String = "Rapat dan luruskan barisan demi kesempurnaan sholat kita.",
    val speed: Int = 5,
    val width: String = "100%"
)

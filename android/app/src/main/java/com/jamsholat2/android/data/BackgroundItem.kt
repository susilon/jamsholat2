package com.jamsholat2.android.data

import kotlinx.serialization.Serializable

@Serializable
data class BackgroundItem(
    val name: String,
    val type: String // "video" or "image"
) {
    companion object {
        fun isVideo(item: BackgroundItem): Boolean = item.type == "video"
        fun isImage(item: BackgroundItem): Boolean = item.type == "image"
        fun fromFileName(fileName: String): BackgroundItem {
            val lower = fileName.lowercase()
            val type = when {
                lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".3gp") -> "video"
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") -> "image"
                else -> "video"
            }
            return BackgroundItem(fileName, type)
        }
    }
}

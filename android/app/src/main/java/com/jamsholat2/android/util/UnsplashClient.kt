package com.jamsholat2.android.util

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class UnsplashSearchResponse(
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    val results: List<UnsplashPhoto> = emptyList()
)

@Serializable
data class UnsplashPhoto(
    val id: String,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("alt_description") val altDescription: String? = null,
    val urls: UnsplashPhotoUrls = UnsplashPhotoUrls(),
    val user: UnsplashUser? = null
)

@Serializable
data class UnsplashPhotoUrls(
    val raw: String? = null,
    val full: String? = null,
    val regular: String? = null,
    val small: String? = null,
    val thumb: String? = null
)

@Serializable
data class UnsplashUser(val name: String? = null)

/**
 * Unsplash public website search API helpers.
 *
 * The endpoint (unsplash.com/napi/search/photos) is the same unauthenticated JSON API
 * the unsplash.com website calls from your browser - no API key involved. However it is
 * gated by an anti-bot JavaScript challenge, so requests must go through a real browser
 * engine (see [UnsplashWebFetcher]) rather than plain HTTP.
 */
object UnsplashClient {

    private const val SEARCH_URL = "https://unsplash.com/napi/search/photos"

    val json = Json { ignoreUnknownKeys = true }

    fun buildSearchUrl(
        query: String,
        page: Int,
        perPage: Int = 30,
        orientation: String = "landscape"
    ): String {
        return Uri.parse(SEARCH_URL).buildUpon()
            .appendQueryParameter("query", query)
            .appendQueryParameter("page", page.toString())
            .appendQueryParameter("per_page", perPage.toString())
            .appendQueryParameter("orientation", orientation)
            .build()
            .toString()
    }

    fun parseSearchResponse(body: String?): UnsplashSearchResponse? {
        if (body.isNullOrBlank()) return null
        return try {
            val parsed = json.decodeFromString<UnsplashSearchResponse>(body)
            // Keep only free photos served from images.unsplash.com. Unsplash+ previews
            // (plus.unsplash.com/premium_photo-...) are premium, watermarked, and often
            // refuse plain hotlinking - useless as downloadable free backgrounds.
            parsed.copy(
                results = parsed.results.filter { photo ->
                    photo.urls.raw?.startsWith("https://images.unsplash.com/") == true ||
                        photo.urls.regular?.startsWith("https://images.unsplash.com/") == true
                }
            )
        } catch (_: Exception) {
            null
        }
    }
}

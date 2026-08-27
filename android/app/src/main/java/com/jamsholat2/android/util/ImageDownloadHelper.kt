package com.jamsholat2.android.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.CookieManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageDownloadHelper {

    private const val REFERER = "https://unsplash.com/"
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    fun isUnsplashImage(url: String): Boolean {
        return url.contains("images.unsplash.com") || url.contains("plus.unsplash.com")
    }

    /**
     * Rewrites an images.unsplash.com URL to a high resolution JPEG.
     * Unsplash CDN (imgix) accepts these query params regardless of what was in the original src.
     */
    fun upgradeToHighRes(url: String): String {
        if (!isUnsplashImage(url)) return url
        return try {
            val uri = Uri.parse(url)
            val builder = uri.buildUpon().query(null)
            builder.appendQueryParameter("w", "1920")
            builder.appendQueryParameter("q", "80")
            builder.appendQueryParameter("fm", "jpg")
            builder.appendQueryParameter("fit", "max")
            builder.build().toString()
        } catch (_: Exception) {
            url
        }
    }

    fun buildFileName(mimeType: String?): String {
        val ext = when {
            mimeType?.contains("png") == true -> "png"
            mimeType?.contains("webp") == true -> "webp"
            mimeType?.contains("gif") == true -> "gif"
            else -> "jpg"
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val rand = (100..999).random()
        return "unsplash_${stamp}_$rand.${ext}"
    }

    /**
     * Writes already-downloaded image bytes into the public Downloads folder.
     * Blocking I/O - call from a background dispatcher. Returns (ok, message).
     */
    fun saveImageBytes(context: Context, bytes: ByteArray, mimeType: String = "image/jpeg"): Pair<Boolean, String> {
        if (bytes.isEmpty()) return false to "data kosong"
        val fileName = buildFileName(mimeType)
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false to "gagal membuat file"
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: return false to "gagal membuka file"
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.outputStream().use { it.write(bytes) }
            }
            true to fileName
        } catch (e: Exception) {
            false to (e.message ?: "gagal menyimpan")
        }
    }

    fun enqueueDownload(
        context: Context,
        rawUrl: String?,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeTypeHint: String? = null
    ): Boolean {
        val url = rawUrl ?: return false
        if (!url.startsWith("http")) return false
        val mimeType = mimeTypeHint?.takeIf { it.isNotBlank() && it != "unknown" } ?: "image/jpeg"
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val fileName = buildFileName(mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                setTitle(fileName)
                setDescription("Gambar latar dari Unsplash")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                addRequestHeader("Referer", REFERER)
                addRequestHeader("User-Agent", userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT)
                try {
                    CookieManager.getInstance()
                        .getCookie(url)?.takeIf { it.isNotBlank() }
                        ?.let { addRequestHeader("Cookie", it) }
                } catch (_: Exception) {
                }
            }
            dm.enqueue(request) > 0L
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Enqueues a download and polls the DownloadManager until it finishes or times out,
     * reporting the real outcome (success / failure reason) via [onFinished] on main thread.
     */
    fun enqueueAndTrackDownload(
        context: Context,
        rawUrl: String?,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeTypeHint: String? = null,
        onFinished: (ok: Boolean, message: String) -> Unit
    ): Long? {
        if (rawUrl == null || !rawUrl.startsWith("http")) {
            onFinished(false, "URL tidak valid")
            return null
        }
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val mimeType = mimeTypeHint?.takeIf { it.isNotBlank() && it != "unknown" } ?: "image/jpeg"
            val fileName = buildFileName(mimeType)
            val request = DownloadManager.Request(Uri.parse(rawUrl)).apply {
                setMimeType(mimeType)
                setTitle(fileName)
                setDescription("Gambar latar dari Unsplash")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                addRequestHeader("Referer", REFERER)
                addRequestHeader(
                    "User-Agent",
                    userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT
                )
                try {
                    CookieManager.getInstance()
                        .getCookie(rawUrl)?.takeIf { it.isNotBlank() }
                        ?.let { addRequestHeader("Cookie", it) }
                } catch (_: Exception) {
                }
            }
            val id = dm.enqueue(request)
            if (id > 0L) {
                trackDownload(context, id, onFinished)
                id
            } else {
                onFinished(false, "Gagal memulai unduhan")
                null
            }
        } catch (e: Exception) {
            onFinished(false, e.message ?: "Gagal mengunduh")
            null
        }
    }

    private const val MAX_POLL_TRIES = 60 // 60 x 2s = 2 minutes
    private val pollHandler = Handler(Looper.getMainLooper())

    fun trackDownload(
        context: Context,
        downloadId: Long,
        onFinished: (ok: Boolean, message: String) -> Unit
    ) {
        pollHandler.post(object : Runnable {
            var tries = 0
            override fun run() {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                try {
                    dm.query(query)?.use { cursor: Cursor ->
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                            )
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    onFinished(true, "Tersimpan di folder Download")
                                    return
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    val reason = cursor.getInt(
                                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                    )
                                    onFinished(false, describeFailure(reason))
                                    return
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    onFinished(false, e.message ?: "Gagal memeriksa unduhan")
                    return
                }
                tries++
                if (tries < MAX_POLL_TRIES) {
                    pollHandler.postDelayed(this, 2000)
                } else {
                    onFinished(false, "Waktu unduhan habis")
                }
            }
        })
    }

    fun describeFailure(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File sudah ada"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Penyimpanan tidak ditemukan"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Ruang penyimpanan tidak cukup"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server menolak (HTTP error)"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP error saat mengunduh"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Terlalu banyak redirect"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Menunggu koneksi jaringan"
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Menunggu WiFi"
            DownloadManager.PAUSED_UNKNOWN -> "Dijeda"
            else -> "Gagal (kode $reason)"
        }
    }
}

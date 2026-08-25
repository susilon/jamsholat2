package com.jamsholat2.android.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.jamsholat2.android.data.BackgroundItem
import java.io.File

object BackgroundFileManager {

    fun getVideoDir(context: Context): File = File(context.filesDir, "videos").apply { mkdirs() }
    fun getImageDir(context: Context): File = File(context.filesDir, "images").apply { mkdirs() }

    fun getFileForItem(context: Context, item: BackgroundItem): File? {
        val dir = if (item.type == "video") getVideoDir(context) else getImageDir(context)
        val f = File(dir, item.name)
        return if (f.exists()) f else null
    }

    fun resolveUriForItem(context: Context, item: BackgroundItem): String? {
        val file = getFileForItem(context, item)
        if (file != null) return Uri.fromFile(file).toString()
        // Fallback to assets: try asset:///videos/ or asset:///images/
        return try {
            val assetPath = if (item.type == "video") "videos/${item.name}" else "images/${item.name}"
            context.assets.list(if (item.type == "video") "videos" else "images")?.let { list ->
                if (list.contains(item.name)) {
                    return "asset:///${assetPath}"
                }
            }
            // Special fallback for tawaf.mp4 which is known to be in assets/videos
            if (item.name == "tawaf.mp4") "asset:///videos/tawaf.mp4" else null
        } catch (_: Exception) {
            null
        }
    }

    fun isImageFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")
    }

    fun isVideoFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".3gp")
    }

    fun getDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    fun copyUriToInternal(context: Context, uri: Uri): BackgroundItem? {
        try {
            val mime = context.contentResolver.getType(uri) ?: ""
            val displayName = getDisplayName(context, uri)
            val extension = when {
                mime.startsWith("video") -> ".mp4"
                mime.startsWith("image") -> {
                    when {
                        mime.contains("png") -> ".png"
                        mime.contains("jpeg") || mime.contains("jpg") -> ".jpg"
                        mime.contains("webp") -> ".webp"
                        mime.contains("gif") -> ".gif"
                        else -> ".jpg"
                    }
                }
                else -> {
                    // infer from displayName
                    val lower = (displayName ?: "").lowercase()
                    when {
                        lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") -> ".mp4"
                        lower.endsWith(".png") -> ".png"
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> ".jpg"
                        lower.endsWith(".webp") -> ".webp"
                        lower.endsWith(".gif") -> ".gif"
                        else -> ".mp4"
                    }
                }
            }
            val isImage = mime.startsWith("image") || extension in listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
            val type = if (isImage) "image" else "video"

            var baseName = displayName?.takeIf { it.isNotBlank() } ?: "${type}_${System.currentTimeMillis()}${extension}"
            // Sanitize
            baseName = baseName.replace("/", "_").replace("\\", "_")
            // Ensure correct extension
            if (!baseName.lowercase().endsWith(extension) && !isImageFile(baseName) && !isVideoFile(baseName)) {
                // keep as is, but add extension if missing
                if (!baseName.contains(".")) baseName += extension
            }

            val dir = if (type == "image") getImageDir(context) else getVideoDir(context)
            var destFile = File(dir, baseName)
            var counter = 1
            while (destFile.exists()) {
                val dot = baseName.lastIndexOf('.')
                val nameWithoutExt = if (dot > 0) baseName.substring(0, dot) else baseName
                val ext = if (dot > 0) baseName.substring(dot) else extension
                destFile = File(dir, "${nameWithoutExt}_$counter$ext")
                counter++
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            return BackgroundItem(destFile.name, type)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deleteItem(context: Context, item: BackgroundItem): Boolean {
        return try {
            val file = getFileForItem(context, item)
            if (file != null && file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    fun copyFileToInternal(context: Context, source: File): BackgroundItem? {
        return try {
            if (!source.exists()) return null
            val isImage = isImageFile(source.name)
            val isVideo = isVideoFile(source.name)
            if (!isImage && !isVideo) return null
            val type = if (isImage) "image" else "video"
            val dir = if (type == "image") getImageDir(context) else getVideoDir(context)
            var destFile = File(dir, source.name)
            var counter = 1
            while (destFile.exists()) {
                val dot = source.name.lastIndexOf('.')
                val nameWithoutExt = if (dot > 0) source.name.substring(0, dot) else source.name
                val ext = if (dot > 0) source.name.substring(dot) else ""
                destFile = File(dir, "${nameWithoutExt}_$counter$ext")
                counter++
            }
            source.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            BackgroundItem(destFile.name, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class ExternalMedia(val name: String, val uri: Uri, val type: String, val file: File? = null)

    fun listExternalMedia(context: Context, typeFilter: String = "all"): List<ExternalMedia> {
        val result = mutableListOf<ExternalMedia>()
        val seenNames = mutableSetOf<String>()
        val resolver = context.contentResolver
        try {
            if (typeFilter == "video" || typeFilter == "all") {
                try {
                    val uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    val projection = arrayOf(
                        android.provider.MediaStore.Video.Media._ID,
                        android.provider.MediaStore.Video.Media.DISPLAY_NAME
                    )
                    resolver.query(uri, projection, null, null, null)?.use { c ->
                        val idIdx = c.getColumnIndex(android.provider.MediaStore.Video.Media._ID)
                        val nameIdx = c.getColumnIndex(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                        while (c.moveToNext()) {
                            val id = if (idIdx >= 0) c.getLong(idIdx) else -1L
                            val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                            if (name.isNotBlank() && isVideoFile(name) && seenNames.add(name)) {
                                val contentUri = if (id >= 0) android.content.ContentUris.withAppendedId(uri, id) else null
                                if (contentUri != null) {
                                    result.add(ExternalMedia(name, contentUri, "video"))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            if (typeFilter == "image" || typeFilter == "all") {
                try {
                    val uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val projection = arrayOf(
                        android.provider.MediaStore.Images.Media._ID,
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME
                    )
                    resolver.query(uri, projection, null, null, null)?.use { c ->
                        val idIdx = c.getColumnIndex(android.provider.MediaStore.Images.Media._ID)
                        val nameIdx = c.getColumnIndex(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                        while (c.moveToNext()) {
                            val id = if (idIdx >= 0) c.getLong(idIdx) else -1L
                            val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                            if (name.isNotBlank() && isImageFile(name) && seenNames.add(name)) {
                                val contentUri = if (id >= 0) android.content.ContentUris.withAppendedId(uri, id) else null
                                if (contentUri != null) {
                                    result.add(ExternalMedia(name, contentUri, "image"))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        // Fallback to File listing for any missing, and to handle files not yet in MediaStore
        val fileList = listExternalFilesViaFile(typeFilter, seenNames)
        for (f in fileList) {
            val type = if (isVideoFile(f.name)) "video" else "image"
            result.add(ExternalMedia(f.name, Uri.fromFile(f), type, f))
        }
        return result.sortedBy { it.name.lowercase() }
    }

    private fun listExternalFilesViaFile(typeFilter: String, seenNames: MutableSet<String>): List<File> {
        val result = mutableListOf<File>()
        val dirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStorageDirectory(),
            File("/sdcard/Download"),
            File("/sdcard/Pictures"),
            File("/sdcard/Movies"),
            File("/sdcard/DCIM"),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Pictures"),
            File("/storage/emulated/0/Movies")
        )
        val seenPaths = mutableSetOf<String>()
        for (dir in dirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { f ->
                        if (f.isFile && f.canRead() && f.length() > 0 && seenNames.add(f.name) && seenPaths.add(f.absolutePath)) {
                            val name = f.name.lowercase()
                            val isVideo = isVideoFile(name)
                            val isImage = isImageFile(name)
                            val matches = when (typeFilter) {
                                "video" -> isVideo
                                "image" -> isImage
                                else -> isVideo || isImage
                            }
                            if (matches) result.add(f)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return result
    }

    fun listExternalFiles(context: Context, typeFilter: String = "all"): List<File> {
        return listExternalMedia(context, typeFilter).mapNotNull { it.file ?: run {
            // For MediaStore entries without file, we still need a File for old API; skip if no file
            null
        } }.toMutableList().also { fileList ->
            // If MediaStore gave us only Uris without files, fallback to file listing
            if (fileList.isEmpty()) {
                fileList.addAll(listExternalFilesViaFile(typeFilter, mutableSetOf()))
            }
        }
    }

    private fun findFileByName(name: String): File? {
        val candidates = listOf(
            File("/sdcard/Download", name),
            File("/storage/emulated/0/Download", name),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name),
            File("/sdcard/Pictures", name),
            File("/sdcard/Movies", name)
        )
        for (f in candidates) if (f.exists() && f.canRead()) return f
        return null
    }
}

package com.example.regibazarapp.data

import android.content.Context
import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DeliveryEvidenceStore(context: Context) {
    private val evidenceDir = File(context.cacheDir, "driver_evidence").apply {
        if (!exists()) mkdirs()
    }

    fun saveJpeg(actionId: String, name: String, bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val file = File(evidenceDir, "${actionId}_${name}.jpg")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        return file.absolutePath
    }

    fun savePng(actionId: String, name: String, bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val file = File(evidenceDir, "${actionId}_${name}.png")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return file.absolutePath
    }

    fun saveText(actionId: String, name: String, value: String?): String? {
        if (value.isNullOrBlank()) return null
        val file = File(evidenceDir, "${actionId}_${name}.txt")
        file.writeText(value, Charsets.UTF_8)
        return file.absolutePath
    }

    fun readText(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return file.takeIf { it.exists() }?.readText(Charsets.UTF_8)
    }

    fun buildPhotoParts(paths: List<String>): List<MultipartBody.Part> =
        paths.mapNotNull { path ->
            val file = File(path)
            if (!file.exists()) return@mapNotNull null

            val contentType = when (file.extension.lowercase()) {
                "png" -> "image/png"
                else -> "image/jpeg"
            }
            MultipartBody.Part.createFormData(
                name = "photos",
                filename = file.name,
                body = file.asRequestBody(contentType.toMediaTypeOrNull())
            )
        }

    fun delete(paths: List<String?>) {
        paths.filterNotNull().forEach { path ->
            runCatching { File(path).delete() }
        }
    }
}

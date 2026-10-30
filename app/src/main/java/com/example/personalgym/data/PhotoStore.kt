package com.example.personalgym.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Progress photos live in the app's private files folder (not the gallery, not backups, not cloud
 * backup). Imports are rotated upright and shrunk so each photo is a few hundred KB.
 */
object PhotoStore {
    private const val MAX_EDGE = 1600
    private const val QUALITY = 85

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** A fresh file + content Uri for the camera app to write into. */
    fun cameraTarget(context: Context): Uri {
        val out = File(File(context.cacheDir, "camera").apply { mkdirs() }, "shot.jpg")
        out.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", out)
    }

    /** Copies [source] into private storage as an upright, shrunk JPEG. */
    suspend fun import(context: Context, source: Uri, pose: PhotoPose, date: Long = System.currentTimeMillis()): ProgressPhoto =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0) { "That file isn't a photo." }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE * 2) }
            val decoded = resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw IllegalArgumentException("Couldn't read that photo.")
            val rotation = resolver.openInputStream(source)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val scale = minOf(1f, MAX_EDGE.toFloat() / max(decoded.width, decoded.height))
            val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val name = "${date}_${pose.name.lowercase()}.jpg"
            file(context, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            if (upright !== decoded) decoded.recycle()
            ProgressPhoto(date, pose, name)
        }

    /** Decodes a photo scaled for display at roughly [targetPx] on its longest edge (cached). */
    suspend fun load(context: Context, name: String, targetPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$name@$targetPx"
        cache.get(key)?.let { return@withContext it }
        val f = file(context, name)
        if (!f.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx)
        })
        bmp?.also { cache.put(key, it) }
    }

    fun delete(context: Context, name: String) {
        file(context, name).delete()
        cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach { cache.remove(it) }
    }
}

/** Largest power-of-two decode step that keeps the longest edge at least [target] px. */
fun sampleSize(width: Int, height: Int, target: Int): Int {
    var size = 1
    while (max(width, height) / (size * 2) >= target) size *= 2
    return size
}

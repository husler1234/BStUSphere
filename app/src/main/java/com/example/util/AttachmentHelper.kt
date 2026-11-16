package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PickedAttachment(
  val name: String,
  val mimeType: String,
  val sizeBytes: Long,
  val imageBase64: String? = null,
  val textPreview: String? = null,
)

object AttachmentHelper {
  private const val MAX_IMAGE_DIM = 720
  private const val MAX_BASE64_LENGTH = 170_000
  private const val MAX_TEXT_PREVIEW = 3500

  suspend fun readAttachment(context: Context, uri: Uri): PickedAttachment? =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        var fileName = "study_file"
        var sizeBytes = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0) {
              fileName = cursor.getString(nameIdx) ?: fileName
            }
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
              sizeBytes = cursor.getLong(sizeIdx)
            }
          }
        }

        var imageBase64: String? = null
        var textPreview: String? = null

        if (mimeType.startsWith("image/")) {
          imageBase64 = compressImageToBase64(context, uri)
        } else if (
          mimeType.startsWith("text/") ||
            fileName.endsWith(".txt", ignoreCase = true) ||
            fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".csv", ignoreCase = true)
        ) {
          resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val buffer = CharArray(MAX_TEXT_PREVIEW)
            val count = reader.read(buffer, 0, MAX_TEXT_PREVIEW)
            if (count > 0) {
              textPreview = String(buffer, 0, count)
            }
          }
        }

        PickedAttachment(
          name = fileName.take(180),
          mimeType = mimeType.take(100),
          sizeBytes = sizeBytes.coerceAtLeast(0L),
          imageBase64 = imageBase64,
          textPreview = textPreview,
        )
      }.getOrNull()
    }

  private fun compressImageToBase64(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val maxSide = max(bounds.outWidth, bounds.outHeight)
    if (maxSide <= 0) return null

    var sampleSize = 1
    while (maxSide / sampleSize > MAX_IMAGE_DIM * 2) {
      sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val rawBitmap = resolver.openInputStream(uri)?.use {
      BitmapFactory.decodeStream(it, null, decodeOpts)
    } ?: return null

    val scale = MAX_IMAGE_DIM.toFloat() / max(rawBitmap.width, rawBitmap.height).coerceAtLeast(1)
    val scaled = if (scale < 1f) {
      Bitmap.createScaledBitmap(
        rawBitmap,
        (rawBitmap.width * scale).roundToInt().coerceAtLeast(1),
        (rawBitmap.height * scale).roundToInt().coerceAtLeast(1),
        true,
      )
    } else {
      rawBitmap
    }

    var quality = 74
    var encoded: String? = null
    while (quality >= 35) {
      val out = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
      val bytes = out.toByteArray()
      val candidate = Base64.encodeToString(bytes, Base64.NO_WRAP)
      if (candidate.length <= MAX_BASE64_LENGTH) {
        encoded = candidate
        break
      }
      quality -= 12
    }
    if (scaled != rawBitmap) rawBitmap.recycle()
    scaled.recycle()
    return encoded
  }

  fun decodeBase64Image(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return runCatching {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
  }

  fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return ""
    return when {
      bytes < 1024 -> "$bytes B"
      bytes < 1024 * 1024 -> "${bytes / 1024} KB"
      else -> String.format("%.1f MB", bytes / (1024f * 1024f))
    }
  }
}

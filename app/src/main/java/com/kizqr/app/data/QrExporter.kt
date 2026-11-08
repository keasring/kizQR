package com.kizqr.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Сохранение PNG в галерею и системный шторинг через FileProvider.
 */
object QrExporter {

    const val MIME_PNG = "image/png"

    /** Сохранить в Pictures/kizQR (MediaStore на API 29+, файл на старых). */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = "kizqr_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/kizQR")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("MediaStore insert failed")
                    resolver.openOutputStream(uri)?.use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            error("compress failed")
                        }
                    } ?: error("output stream null")
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } else {
                    val dir = File(
                        android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_PICTURES,
                        ),
                        "kizQR",
                    )
                    dir.mkdirs()
                    val f = File(dir, name)
                    f.outputStream().use { out ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            error("compress failed")
                        }
                    }
                    // Публикуем файл в медиатеку
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, f.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(f)
                }
            }
        }

    /** Intent.ACTION_SEND с PNG из кеша. */
    suspend fun shareIntent(context: Context, bitmap: Bitmap): Result<Intent> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "share").apply { mkdirs() }
                val file = File(dir, "kizqr_${System.currentTimeMillis()}.png")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = MIME_PNG
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }
}

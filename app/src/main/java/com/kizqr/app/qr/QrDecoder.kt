package com.kizqr.app.qr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Декодирование QR из изображения (галерея) через ML Kit.
 */
object QrDecoder {

    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    suspend fun decodeFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            ?: return@withContext null
        process(image).firstOrNull()?.rawValue
    }

    suspend fun decodeFromBitmap(bitmap: Bitmap): String? =
        process(InputImage.fromBitmap(bitmap, 0)).firstOrNull()?.rawValue

    private suspend fun process(image: InputImage): List<Barcode> =
        suspendCancellableCoroutine { cont ->
            scanner.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }
}

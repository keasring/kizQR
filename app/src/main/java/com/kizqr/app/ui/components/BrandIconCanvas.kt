package com.kizqr.app.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.kizqr.app.qr.BrandIcons
import com.kizqr.app.qr.LogoPreset

/**
 * Брендовый глиф (Telegram/WhatsApp/…) через общий BrandIcons —
 * тот же код, что рисует логотип в QR.
 */
@Composable
fun BrandIconCanvas(
    logo: LogoPreset,
    tint: Int,
    knockout: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawIntoCanvas { composeCanvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
            val knockoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = knockout }
            BrandIcons.draw(
                composeCanvas.nativeCanvas,
                logo,
                RectF(0f, 0f, size.width, size.height),
                paint,
                knockoutPaint,
            )
        }
    }
}

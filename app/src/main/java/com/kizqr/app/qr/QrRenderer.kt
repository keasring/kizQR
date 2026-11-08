package com.kizqr.app.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * Рендер QR-кода: матрица ZXing → стилизованный Bitmap.
 * Формы точек и «глаз», градиенты, логотип — всё здесь.
 */
object QrRenderer {

    fun render(payload: String, style: QrStyle, sizePx: Int, logoBitmap: Bitmap? = null): Bitmap {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val ec = ErrorCorrectionLevel.valueOf(style.effectiveEc().name)
        val matrix = Encoder.encode(payload, ec, hints).matrix
        val n = matrix.width

        val margin = if (style.quietZone) 4 else 1
        val total = n + margin * 2
        val scale = sizePx.toFloat() / total

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!style.transparentBg) {
            canvas.drawColor(style.bgColor)
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { applyFill(style, sizePx) }
        val knockoutColor = if (style.transparentBg) 0xFFFFFFFF.toInt() else style.bgColor
        val knockoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = knockoutColor }

        drawModules(canvas, matrix, n, margin, scale, fillPaint, style)
        drawEyes(canvas, n, margin, scale, style)
        drawLogo(canvas, style, sizePx, fillPaint, knockoutPaint, logoBitmap)

        return bitmap
    }

    private fun Paint.applyFill(style: QrStyle, size: Int) {
        when (style.colorMode) {
            QrColorMode.SOLID -> color = style.color1
            QrColorMode.LINEAR -> shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                style.color1, style.color2, Shader.TileMode.CLAMP,
            )
            QrColorMode.RADIAL -> shader = RadialGradient(
                size / 2f, size / 2f, size * 0.72f,
                style.color1, style.color2, Shader.TileMode.CLAMP,
            )
        }
    }

    private fun inFinderZone(x: Int, y: Int, n: Int): Boolean =
        (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)

    private fun drawModules(
        canvas: Canvas,
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        n: Int,
        margin: Int,
        scale: Float,
        paint: Paint,
        style: QrStyle,
    ) {
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (matrix.get(x, y).toInt() != 1 || inFinderZone(x, y, n)) continue
                val left = (margin + x) * scale
                val top = (margin + y) * scale
                drawModuleShape(canvas, left, top, scale, style.dotStyle, paint)
            }
        }
    }

    /** Публично: используется и для превью форм в редакторе стилей. */
    fun drawModuleShape(canvas: Canvas, left: Float, top: Float, size: Float, style: DotStyle, paint: Paint) {
        val right = left + size
        val bottom = top + size
        when (style) {
            DotStyle.SQUARE -> canvas.drawRect(left, top, right, bottom, paint)
            DotStyle.ROUNDED -> canvas.drawRoundRect(
                left, top, right, bottom, size * 0.32f, size * 0.32f, paint,
            )
            DotStyle.DOTS -> canvas.drawCircle(
                left + size / 2f, top + size / 2f, size * 0.44f, paint,
            )
            DotStyle.CLASSY -> canvas.drawPath(
                radiiPath(
                    RectF(left, top, right, bottom),
                    floatArrayOf(size * 0.5f, size * 0.5f, 0f, 0f, size * 0.5f, size * 0.5f, 0f, 0f),
                ),
                paint,
            )
            DotStyle.CLASSY_ROUNDED -> canvas.drawPath(
                radiiPath(
                    RectF(left, top, right, bottom),
                    floatArrayOf(size * 0.5f, size * 0.5f, size * 0.18f, size * 0.18f, size * 0.5f, size * 0.5f, size * 0.18f, size * 0.18f),
                ),
                paint,
            )
        }
    }

    /** Скруглённый прямоугольник с индивидуальными радиусами углов (8 значений). */
    private fun radiiPath(rect: RectF, radii: FloatArray): Path =
        Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }

    /**
     * «Глаза» рисуются отдельно: внешнее кольцо — обводкой, внутренний
     * квадрат 3×3 — заливкой. Так не нужны вырезы, работает и с прозрачным фоном.
     */
    private fun drawEyes(canvas: Canvas, n: Int, margin: Int, scale: Float, style: QrStyle) {
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = style.eyeColor ?: style.color1
        }
        val positions = listOf(0 to 0, (n - 7) to 0, 0 to (n - 7))
        for ((fx, fy) in positions) {
            val left = (margin + fx) * scale
            val top = (margin + fy) * scale
            drawEye(canvas, left, top, scale, style.eyeStyle, eyePaint)
        }
    }

    /** Публично: используется и для превью форм в редакторе стилей. */
    fun drawEye(canvas: Canvas, left: Float, top: Float, module: Float, eyeStyle: EyeStyle, paint: Paint) {
        val s = module // размер одного модуля глаза
        val ring = Paint(paint).apply { this.style = Paint.Style.STROKE; strokeWidth = s }
        val cx = left + 3.5f * s
        val cy = top + 3.5f * s
        // Внешнее кольцо 7×7
        when (eyeStyle) {
            EyeStyle.SQUARE -> canvas.drawRoundRect(
                RectF(left + s / 2f, top + s / 2f, left + 6.5f * s, top + 6.5f * s),
                0.35f * s, 0.35f * s, ring,
            )
            EyeStyle.ROUNDED -> canvas.drawRoundRect(
                RectF(left + s / 2f, top + s / 2f, left + 6.5f * s, top + 6.5f * s),
                1.9f * s, 1.9f * s, ring,
            )
            EyeStyle.DOTS -> canvas.drawCircle(cx, cy, 3f * s, ring)
            EyeStyle.LEAF -> canvas.drawPath(
                radiiPath(
                    RectF(left + s / 2f, top + s / 2f, left + 6.5f * s, top + 6.5f * s),
                    floatArrayOf(0f, 0f, 2.1f * s, 2.1f * s, 2.1f * s, 2.1f * s, 2.1f * s, 2.1f * s),
                ),
                ring,
            )
        }
        // Внутренний квадрат 3×3
        val inner = RectF(left + 2f * s, top + 2f * s, left + 5f * s, top + 5f * s)
        val fill = Paint(paint).apply { this.style = Paint.Style.FILL }
        when (eyeStyle) {
            EyeStyle.SQUARE -> canvas.drawRect(inner, fill)
            EyeStyle.ROUNDED -> canvas.drawRoundRect(inner, 0.9f * s, 0.9f * s, fill)
            EyeStyle.DOTS -> canvas.drawCircle(cx, cy, 1.35f * s, fill)
            EyeStyle.LEAF -> canvas.drawPath(
                radiiPath(
                    inner,
                    floatArrayOf(0f, 0f, 1.05f * s, 1.05f * s, 1.05f * s, 1.05f * s, 1.05f * s, 1.05f * s),
                ),
                fill,
            )
        }
    }

    private fun drawLogo(
        canvas: Canvas,
        style: QrStyle,
        sizePx: Int,
        fillPaint: Paint,
        knockoutPaint: Paint,
        logoBitmap: Bitmap?,
    ) {
        if (!style.hasLogo && logoBitmap == null) return
        val boxSize = sizePx * 0.26f
        val box = RectF(
            (sizePx - boxSize) / 2f,
            (sizePx - boxSize) / 2f,
            (sizePx + boxSize) / 2f,
            (sizePx + boxSize) / 2f,
        )
        // Подложка под логотипом: повышает читаемость
        canvas.drawRoundRect(box, boxSize * 0.30f, boxSize * 0.30f, knockoutPaint)

        val inset = boxSize * 0.14f
        val inner = RectF(box.left + inset, box.top + inset, box.right - inset, box.bottom - inset)

        val photo = logoBitmap
        if (photo != null) {
            // вписать по центру с сохранением пропорций
            val dst = RectF(inner)
            if (photo.width > photo.height) {
                val w = inner.height() * photo.width / photo.height
                dst.inset((inner.width() - w) / 2f, 0f)
            } else {
                val h = inner.width() * photo.height / photo.width
                dst.inset(0f, (inner.height() - h) / 2f)
            }
            canvas.drawBitmap(
                photo, null, dst,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
        } else if (style.logo != LogoPreset.NONE) {
            BrandIcons.draw(canvas, style.logo, inner, fillPaint, knockoutPaint)
        }
    }
}

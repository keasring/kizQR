package com.kizqr.app.qr

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser

/**
 * Отрисовка оригинальных брендов на android.graphics.Canvas.
 * Глифы — path data 24x24 из Simple Icons (см. BrandIconPaths), масштабируются в box.
 * Используется и в рендерере QR (логотип в центре), и в Compose-превью
 * через drawIntoCanvas { it.nativeCanvas }.
 */
object BrandIcons {

    private const val UNIT = 24f

    private val basePaths: Map<LogoPreset, Path> by lazy {
        mapOf(
            LogoPreset.TELEGRAM to parse(BrandIconPaths.TELEGRAM),
            LogoPreset.WHATSAPP to parse(BrandIconPaths.WHATSAPP),
            LogoPreset.VK to parse(BrandIconPaths.VK),
            LogoPreset.INSTAGRAM to parse(BrandIconPaths.INSTAGRAM),
            LogoPreset.YOUTUBE to parse(BrandIconPaths.YOUTUBE),
            LogoPreset.SPOTIFY to parse(BrandIconPaths.SPOTIFY),
        )
    }

    /**
     * @param paint    краска основного цвета (сплошной цвет или градиент-шейдер);
     *                 вырезы внутри глифа получаются правилом заливки самого пути
     * @param knockout зарезервировано (вырезы больше не требуют краски фона)
     */
    fun draw(canvas: Canvas, logo: LogoPreset, box: RectF, paint: Paint, knockout: Paint) {
        val base = basePaths[logo] ?: return
        val s = minOf(box.width(), box.height()) / UNIT
        val dx = box.left + (box.width() - UNIT * s) / 2f
        val dy = box.top + (box.height() - UNIT * s) / 2f
        val path = Path(base)
        path.transform(
            Matrix().apply {
                setScale(s, s)
                postTranslate(dx, dy)
            },
        )
        canvas.drawPath(path, paint)
    }

    /**
     * Нормализация SVG path data для androidx PathParser: тот требует строго
     * разделённые токены, а Simple Icons используют допустимые в SVG слитные
     * записи — числа без разделителей ("9.489.004.729-.003") и слитные
     * arc-флаги ("a9.87 9.87 0 01-5.031"). Здесь всё разрежается токенами,
     * arc-флаги читаются по одному символу.
     */
    internal fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length + 64)
        var i = 0
        val n = raw.length
        var cmd = ' '
        var argIdx = 0
        while (i < n) {
            val c = raw[i]
            when {
                c.isWhitespace() || c == ',' -> { i++ }
                c.isLetter() && c in "MmLlHhVvCcSsQqTtAaZz" -> {
                    cmd = c
                    argIdx = 0
                    sb.append(' ').append(c)
                    i++
                }
                else -> {
                    val arc = cmd == 'A' || cmd == 'a'
                    val pos = if (arc) argIdx % 7 else argIdx
                    if (arc && (pos == 3 || pos == 4)) {
                        // large-arc / sweep — одиночный символ 0 или 1
                        require(c == '0' || c == '1') { "Bad arc flag at $i in: $raw" }
                        sb.append(' ').append(c)
                        argIdx++
                        i++
                    } else {
                        val token = readNumber(raw, i) ?: error("Bad number at $i in: $raw")
                        sb.append(' ').append(token)
                        argIdx++
                        i += token.length
                    }
                }
            }
        }
        return sb.toString()
    }

    /** Читает одно число (со знаком, точкой и экспонентой), слитность ок. */
    private fun readNumber(raw: String, start: Int): String? {
        var j = start
        val n = raw.length
        if (j < n && (raw[j] == '+' || raw[j] == '-')) j++
        var digits = 0
        var dots = 0
        while (j < n) {
            val c = raw[j]
            when {
                c.isDigit() -> { digits++; j++ }
                c == '.' && dots == 0 -> { dots++; j++ }
                (c == 'e' || c == 'E') && digits > 0 -> {
                    var k = j + 1
                    if (k < n && (raw[k] == '+' || raw[k] == '-')) k++
                    if (k < n && raw[k].isDigit()) {
                        j = k
                        while (j < n && raw[j].isDigit()) j++
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
        return if (digits == 0) null else raw.substring(start, j)
    }

    private fun parse(data: String): Path = PathParser.createPathFromPathData(normalize(data))
}

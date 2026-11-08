package com.kizqr.app.qr

/** Форма модулей QR-кода. */
enum class DotStyle {
    SQUARE,      // квадраты
    ROUNDED,     // скруглённые квадраты
    DOTS,        // точки
    CLASSY,      // «классика»: острые углы по диагонали
    CLASSY_ROUNDED,
}

/** Форма «глаз» (три угловых маркера). */
enum class EyeStyle {
    SQUARE,
    ROUNDED,
    DOTS,
    LEAF,        // один острый угол, остальные скруглены
}

enum class QrColorMode { SOLID, LINEAR, RADIAL }

/** Логотип в центре: пресет бренда или фото пользователя. */
enum class LogoPreset { NONE, TELEGRAM, WHATSAPP, VK, INSTAGRAM, YOUTUBE, SPOTIFY }

enum class QrErrorCorrection(val label: String, val recovery: String) {
    L("L · 7%", "7% восстановления"),
    M("M · 15%", "15% восстановления"),
    Q("Q · 25%", "25% восстановления"),
    H("H · 30%", "30% восстановления"),
}

/**
 * Стиль отображения QR-кода. Цвета — ARGB int, чтобы модель оставалась
 * чистым Kotlin (сериализация, юнит-тесты).
 */
data class QrStyle(
    val dotStyle: DotStyle = DotStyle.ROUNDED,
    val eyeStyle: EyeStyle = EyeStyle.ROUNDED,
    val colorMode: QrColorMode = QrColorMode.SOLID,
    val color1: Int = 0xFF17171F.toInt(),
    val color2: Int = 0xFFF97316.toInt(),
    val eyeColor: Int? = null,
    val bgColor: Int = 0xFFFFFFFF.toInt(),
    val transparentBg: Boolean = false,
    val logo: LogoPreset = LogoPreset.NONE,
    val logoUri: String? = null,
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.M,
    val quietZone: Boolean = true,
) {
    val hasLogo: Boolean get() = logo != LogoPreset.NONE || logoUri != null

    fun effectiveEc(): QrErrorCorrection = if (hasLogo) QrErrorCorrection.H else errorCorrection

    /** Компактное кодирование для истории: v1|v2|v3|... */
    fun encode(): String {
        val fields = listOf(
            dotStyle.name,
            eyeStyle.name,
            colorMode.name,
            color1.toString(),
            color2.toString(),
            eyeColor?.toString() ?: "-",
            bgColor.toString(),
            if (transparentBg) "1" else "0",
            logo.name,
            (logoUri ?: "-").replace("|", "%7C"),
            errorCorrection.name,
            if (quietZone) "1" else "0",
        )
        return fields.joinToString("|")
    }

    companion object {
        fun decode(encoded: String): QrStyle {
            val f = encoded.split("|")
            if (f.size < 12) return QrStyle()
            return runCatching {
                QrStyle(
                    dotStyle = DotStyle.valueOf(f[0]),
                    eyeStyle = EyeStyle.valueOf(f[1]),
                    colorMode = QrColorMode.valueOf(f[2]),
                    color1 = f[3].toInt(),
                    color2 = f[4].toInt(),
                    eyeColor = f[5].takeIf { it != "-" }?.toInt(),
                    bgColor = f[6].toInt(),
                    transparentBg = f[7] == "1",
                    logo = LogoPreset.valueOf(f[8]),
                    logoUri = f[9].takeIf { it != "-" }?.replace("%7C", "|"),
                    errorCorrection = QrErrorCorrection.valueOf(f[10]),
                    quietZone = f[11] == "1",
                )
            }.getOrDefault(QrStyle())
        }
    }
}

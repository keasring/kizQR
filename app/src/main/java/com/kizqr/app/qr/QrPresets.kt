package com.kizqr.app.qr

import android.graphics.Color

data class ServicePreset(
    val id: LogoPreset,
    val name: String,
    val style: QrStyle,
)

/**
 * Фирменные стили сервисов. Идентификатор пресета совпадает с LogoPreset,
 * потому что пресет и логотип идут в паре.
 */
object QrPresets {

    val all: List<ServicePreset> = listOf(
        ServicePreset(
            LogoPreset.TELEGRAM, "Telegram",
            QrStyle(
                dotStyle = DotStyle.DOTS,
                eyeStyle = EyeStyle.ROUNDED,
                color1 = 0xFF229ED9.toInt(),
                eyeColor = 0xFF229ED9.toInt(),
                logo = LogoPreset.TELEGRAM,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
        ServicePreset(
            LogoPreset.WHATSAPP, "WhatsApp",
            QrStyle(
                dotStyle = DotStyle.DOTS,
                eyeStyle = EyeStyle.ROUNDED,
                color1 = 0xFF25D366.toInt(),
                eyeColor = 0xFF25D366.toInt(),
                logo = LogoPreset.WHATSAPP,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
        ServicePreset(
            LogoPreset.VK, "VK",
            QrStyle(
                dotStyle = DotStyle.ROUNDED,
                eyeStyle = EyeStyle.ROUNDED,
                color1 = 0xFF0077FF.toInt(),
                eyeColor = 0xFF0077FF.toInt(),
                logo = LogoPreset.VK,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
        ServicePreset(
            LogoPreset.INSTAGRAM, "Instagram",
            QrStyle(
                dotStyle = DotStyle.DOTS,
                eyeStyle = EyeStyle.ROUNDED,
                colorMode = QrColorMode.LINEAR,
                color1 = 0xFFF58529.toInt(),
                color2 = 0xFF8134AF.toInt(),
                eyeColor = 0xFFDD2A7B.toInt(),
                logo = LogoPreset.INSTAGRAM,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
        ServicePreset(
            LogoPreset.YOUTUBE, "YouTube",
            QrStyle(
                dotStyle = DotStyle.ROUNDED,
                eyeStyle = EyeStyle.SQUARE,
                color1 = 0xFFFF0000.toInt(),
                eyeColor = 0xFFFF0000.toInt(),
                logo = LogoPreset.YOUTUBE,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
        ServicePreset(
            LogoPreset.SPOTIFY, "Spotify",
            QrStyle(
                dotStyle = DotStyle.DOTS,
                eyeStyle = EyeStyle.DOTS,
                color1 = 0xFF1DB954.toInt(),
                eyeColor = 0xFF1DB954.toInt(),
                bgColor = 0xFF191414.toInt(),
                logo = LogoPreset.SPOTIFY,
                errorCorrection = QrErrorCorrection.Q,
            ),
        ),
    )

    val byId: Map<LogoPreset, ServicePreset> = all.associateBy { it.id }

    /**
     * Определение сервиса по ссылке — для автоподсказки пресета.
     * Возвращает null, если сервис не распознан.
     */
    fun detect(url: String): LogoPreset? {
        val host = runCatching { java.net.URI(url.lowercase()).host }.getOrNull() ?: url.lowercase()
        return when {
            host.contains("t.me") || host.contains("telegram") -> LogoPreset.TELEGRAM
            host.contains("wa.me") || host.contains("whatsapp") -> LogoPreset.WHATSAPP
            host.contains("vk.com") || host.contains("vk.ru") -> LogoPreset.VK
            host.contains("instagram.com") -> LogoPreset.INSTAGRAM
            host.contains("youtube.com") || host.contains("youtu.be") -> LogoPreset.YOUTUBE
            host.contains("spotify.com") -> LogoPreset.SPOTIFY
            else -> null
        }
    }
}

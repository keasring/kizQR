package com.kizqr.app.qr

/**
 * Тип контента QR-кода. Чистый Kotlin (без Android) — покрывается юнит-тестами.
 */
enum class QrContentType(val title: String) {
    TEXT("Текст"),
    URL("Ссылка"),
    WIFI("Wi-Fi"),
    CONTACT("Контакт"),
    SMS("SMS"),
    PHONE("Телефон"),
    GEO("Гео"),
}

sealed interface QrContent {
    val kind: QrContentType
    val summary: String

    data class Text(val text: String) : QrContent {
        override val kind = QrContentType.TEXT
        override val summary = text
    }

    data class Url(val url: String) : QrContent {
        override val kind = QrContentType.URL
        override val summary = url
    }

    enum class WifiEncryption(val label: String) { NOPASS("Без пароля"), WPA("WPA/WPA2/WPA3"), WEP("WEP") }

    data class Wifi(
        val ssid: String,
        val password: String = "",
        val encryption: WifiEncryption = WifiEncryption.WPA,
        val hidden: Boolean = false,
    ) : QrContent {
        override val kind = QrContentType.WIFI
        override val summary = ssid
    }

    data class Contact(
        val firstName: String = "",
        val lastName: String = "",
        val org: String = "",
        val phone: String = "",
        val email: String = "",
        val website: String = "",
        val note: String = "",
    ) : QrContent {
        override val kind = QrContentType.CONTACT
        override val summary = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { phone.ifBlank { email } }
    }

    data class Sms(val phone: String, val message: String) : QrContent {
        override val kind = QrContentType.SMS
        override val summary = phone
    }

    data class Phone(val number: String) : QrContent {
        override val kind = QrContentType.PHONE
        override val summary = number
    }

    data class Geo(val lat: String, val lng: String, val label: String = "") : QrContent {
        override val kind = QrContentType.GEO
        override val summary = "$lat, $lng"
    }
}

/**
 * Строка, которая кодируется в QR-код.
 */
fun QrContent.payload(): String = when (this) {
    is QrContent.Text -> text.trim()
    is QrContent.Url -> normalizeUrl(url.trim())
    is QrContent.Wifi -> buildString {
        append("WIFI:T:").append(encryption.name.replace("NOPASS", "nopass")).append(";")
        append("S:").append(escapeWifi(ssid)).append(";")
        if (encryption != QrContent.WifiEncryption.NOPASS && password.isNotEmpty()) {
            append("P:").append(escapeWifi(password)).append(";")
        }
        if (hidden) append("H:true;")
        append(";")
    }
    is QrContent.Contact -> buildString {
        append("BEGIN:VCARD").append("\n")
        append("VERSION:3.0").append("\n")
        append("N:").append(escapeVCard(lastName)).append(";").append(escapeVCard(firstName)).append(";;;").append("\n")
        val fn = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
        if (fn.isNotEmpty()) append("FN:").append(escapeVCard(fn)).append("\n")
        if (org.isNotBlank()) append("ORG:").append(escapeVCard(org)).append("\n")
        if (phone.isNotBlank()) append("TEL;TYPE=CELL:").append(escapeVCard(phone)).append("\n")
        if (email.isNotBlank()) append("EMAIL:").append(escapeVCard(email)).append("\n")
        if (website.isNotBlank()) append("URL:").append(escapeVCard(website)).append("\n")
        if (note.isNotBlank()) append("NOTE:").append(escapeVCard(note)).append("\n")
        append("END:VCARD")
    }
    is QrContent.Sms -> buildString {
        append("SMSTO:").append(phone.trim()).append(":").append(message)
    }
    is QrContent.Phone -> "tel:" + number.replace(" ", "").replace("-", "")
    is QrContent.Geo -> buildString {
        append("geo:").append(lat.trim()).append(",").append(lng.trim())
        if (label.isNotBlank()) append("?q=").append(lat.trim()).append(",").append(lng.trim())
            .append("(").append(label).append(")")
    }
}

private fun normalizeUrl(url: String): String = when {
    url.startsWith("http://") || url.startsWith("https://") -> url
    else -> "https://$url"
}

// Экранирование по спецификации WIFI: (https://github.com/zxing/zxing/wiki/Barcode-Contents)
private fun escapeWifi(s: String): String =
    buildString {
        for (c in s) {
            if (c in "\\\\;,\":") append('\\')
            append(c)
        }
    }

private fun escapeVCard(s: String): String = s.replace("\\", "\\\\").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")

private fun unescapeWifi(s: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '\\' && i + 1 < s.length) {
            sb.append(s[i + 1]); i += 2
        } else {
            sb.append(c); i += 1
        }
    }
    return sb.toString()
}

/**
 * Разбор отсканированного текста в типизированный контент.
 */
fun parseScanned(raw: String): QrContent {
    val text = raw.trim()
    if (text.isEmpty()) return QrContent.Text("")
    val upper = text.uppercase()
    return when {
        upper.startsWith("WIFI:") -> parseWifi(text)
        upper.startsWith("BEGIN:VCARD") -> parseVCard(text)
        upper.startsWith("SMSTO:") || upper.startsWith("MMSTO:") -> parseSms(text)
        upper.startsWith("SMS:") -> parseSms(text)
        upper.startsWith("TEL:") -> QrContent.Phone(text.substring(4).trim())
        upper.startsWith("GEO:") -> parseGeo(text)
        upper.startsWith("HTTP://") || upper.startsWith("HTTPS://") -> QrContent.Url(text)
        else -> {
            // «Голый» домен — считаем ссылкой
            if (looksLikeUrl(text)) QrContent.Url(text) else QrContent.Text(text)
        }
    }
}

private fun looksLikeUrl(text: String): Boolean {
    val dot = text.indexOf('.', 1)
    if (dot <= 0 || dot >= text.length - 1) return false
    return text.none { it.isWhitespace() } && !text.contains('@')
}

private fun parseWifi(text: String): QrContent.Wifi {
    val body = text.substringAfter("WIFI:", "").trimEnd(';')
    var type = "WPA"; var ssid = ""; var pass = ""; var hidden = false
    var i = 0
    val sb = StringBuilder()
    fun flushField() {
        val field = sb.toString(); sb.clear()
        if (field.length >= 2 && field[1] == ':') {
            when (field[0].uppercase()) {
                "T" -> type = field.substring(2)
                "S" -> ssid = field.substring(2)
                "P" -> pass = field.substring(2)
                "H" -> hidden = field.substring(2).equals("true", ignoreCase = true)
            }
        }
    }
    while (i < body.length) {
        val c = body[i]
        if (c == '\\' && i + 1 < body.length) { sb.append(body[i + 1]); i += 2; continue }
        if (c == ';') {
            flushField()
        } else sb.append(c)
        i++
    }
    if (sb.isNotEmpty()) flushField()
    val enc = when (type.uppercase()) {
        "NOPASS" -> QrContent.WifiEncryption.NOPASS
        "WEP" -> QrContent.WifiEncryption.WEP
        else -> QrContent.WifiEncryption.WPA
    }
    return QrContent.Wifi(ssid = ssid, password = if (enc == QrContent.WifiEncryption.NOPASS) "" else pass, encryption = enc, hidden = hidden)
}

private fun parseVCard(text: String): QrContent.Contact {
    var first = ""; var last = ""; var org = ""; var phone = ""; var email = ""; var website = ""; var note = ""
    for (line in text.lines()) {
        val trimmed = line.trim()
        val idx = trimmed.indexOf(':')
        if (idx <= 0) continue
        val key = trimmed.substring(0, idx).uppercase()
        val value = trimmed.substring(idx + 1)
        when {
            key == "N" -> {
                val parts = value.split(';')
                if (parts.isNotEmpty()) last = unescapeVCard(parts[0])
                if (parts.size > 1) first = unescapeVCard(parts[1])
            }
            key == "FN" -> if (first.isEmpty() && last.isEmpty()) {
                val parts = value.split(' ', limit = 2)
                if (parts.isNotEmpty()) first = unescapeVCard(parts[0])
                if (parts.size > 1) last = unescapeVCard(parts[1])
            }
            key.startsWith("TEL") -> if (phone.isEmpty()) phone = value
            key.startsWith("EMAIL") -> if (email.isEmpty()) email = value
            key == "ORG" -> if (org.isEmpty()) org = unescapeVCard(value)
            key == "URL" -> if (website.isEmpty()) website = value
            key == "NOTE" -> if (note.isEmpty()) note = unescapeVCard(value)
        }
    }
    return QrContent.Contact(firstName = first, lastName = last, org = org, phone = phone, email = email, website = website, note = note)
}

private fun unescapeVCard(s: String): String =
    s.replace("\\n", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

private fun parseSms(text: String): QrContent.Sms {
    val rest = text.substringAfter(':', "")
    val idx = rest.indexOf(':')
    return if (idx >= 0) {
        QrContent.Sms(phone = rest.substring(0, idx).trim(), message = rest.substring(idx + 1))
    } else {
        QrContent.Sms(phone = rest.trim(), message = "")
    }
}

private fun parseGeo(text: String): QrContent.Geo {
    val rest = text.substringAfter("geo:", "")
    val (coords, label) = rest.split('?').let { it[0] to it.getOrElse(1) { "" } }
    val latlng = coords.split(',')
    val lat = latlng.getOrNull(0)?.trim() ?: ""
    val lng = latlng.getOrNull(1)?.trim() ?: ""
    val l = label.substringAfter("q=", "").substringAfter('(').substringBefore(')')
    return QrContent.Geo(lat = lat, lng = lng, label = l)
}

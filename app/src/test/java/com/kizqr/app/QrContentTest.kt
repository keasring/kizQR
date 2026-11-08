package com.kizqr.app

import com.kizqr.app.qr.QrContent
import com.kizqr.app.qr.QrContentType
import com.kizqr.app.qr.QrStyle
import com.kizqr.app.qr.parseScanned
import com.kizqr.app.qr.payload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrContentTest {

    @Test
    fun `wifi payload содержит ssid, пароль и тип шифрования`() {
        val wifi = QrContent.Wifi("HomeNet", "s3cret!", QrContent.WifiEncryption.WPA)
        assertEquals("WIFI:T:WPA;S:HomeNet;P:s3cret!;;", wifi.payload())
    }

    @Test
    fun `wifi payload экранирует спецсимволы`() {
        val wifi = QrContent.Wifi("a;b,c:d\"e\\f", "p1;2", QrContent.WifiEncryption.WPA)
        assertEquals("WIFI:T:WPA;S:a\\;b\\,c\\:d\\\"e\\\\f;P:p1\\;2;;", wifi.payload())
    }

    @Test
    fun `wifi без пароля не содержит P-поле`() {
        val wifi = QrContent.Wifi("OpenNet", "", QrContent.WifiEncryption.NOPASS)
        assertEquals("WIFI:T:nopass;S:OpenNet;;", wifi.payload())
    }

    @Test
    fun `wifi скрытая сеть содержит H`() {
        val wifi = QrContent.Wifi("Hidden", "pw", QrContent.WifiEncryption.WPA, hidden = true)
        assertEquals("WIFI:T:WPA;S:Hidden;P:pw;H:true;;", wifi.payload())
    }

    @Test
    fun `parse wifi возвращает поля обратно`() {
        val original = QrContent.Wifi("My;Net", "pass;word", QrContent.WifiEncryption.WPA)
        val parsed = parseScanned(original.payload())
        assertTrue(parsed is QrContent.Wifi)
        val wifi = parsed as QrContent.Wifi
        assertEquals("My;Net", wifi.ssid)
        assertEquals("pass;word", wifi.password)
        assertEquals(QrContent.WifiEncryption.WPA, wifi.encryption)
    }

    @Test
    fun `vCard содержит ключевые поля`() {
        val contact = QrContent.Contact(
            firstName = "Иван", lastName = "Иванов",
            org = "kizQR", phone = "+79990001122", email = "ivan@example.com",
        )
        val p = contact.payload()
        assertTrue(p.startsWith("BEGIN:VCARD"))
        assertTrue(p.contains("N:Иванов;Иван;;;"))
        assertTrue(p.contains("FN:Иван Иванов"))
        assertTrue(p.contains("TEL;TYPE=CELL:+79990001122"))
        assertTrue(p.contains("EMAIL:ivan@example.com"))
        assertTrue(p.endsWith("END:VCARD"))
    }

    @Test
    fun `parse vcard возвращает поля обратно`() {
        val original = QrContent.Contact(
            firstName = "Мария", lastName = "Петрова",
            phone = "+70001112233", email = "m@example.com", org = "OOO Test",
        )
        val parsed = parseScanned(original.payload())
        assertTrue(parsed is QrContent.Contact)
        val c = parsed as QrContent.Contact
        assertEquals("Мария", c.firstName)
        assertEquals("Петрова", c.lastName)
        assertEquals("+70001112233", c.phone)
        assertEquals("m@example.com", c.email)
    }

    @Test
    fun `sms payload`() {
        val sms = QrContent.Sms("+79991234567", "Привет")
        assertEquals("SMSTO:+79991234567:Привет", sms.payload())
        val parsed = parseScanned(sms.payload())
        assertEquals(QrContent.Sms("+79991234567", "Привет"), parsed)
    }

    @Test
    fun `phone payload убирает пробелы`() {
        val phone = QrContent.Phone("+7 999 123-45-67")
        assertEquals("tel:+79991234567", phone.payload())
        val parsed = parseScanned(phone.payload())
        assertEquals(QrContent.Phone("+79991234567"), parsed)
    }

    @Test
    fun `geo payload`() {
        val geo = QrContent.Geo("55.7558", "37.6173")
        assertEquals("geo:55.7558,37.6173", geo.payload())
        val parsed = parseScanned(geo.payload())
        assertEquals(QrContent.Geo("55.7558", "37.6173"), parsed)
    }

    @Test
    fun `url дополняется схемой и распознаётся`() {
        val url = QrContent.Url("example.com/path")
        assertEquals("https://example.com/path", url.payload())
        assertEquals(QrContent.Url("https://example.com/path"), parseScanned(url.payload()))
    }

    @Test
    fun `telegram ссылка распознаётся как url`() {
        assertEquals(QrContent.Url("https://t.me/durov"), parseScanned("https://t.me/durov"))
    }

    @Test
    fun `произвольный текст остаётся текстом`() {
        val text = QrContent.Text("Просто текст с переносом\nвторая строка")
        assertEquals(text, parseScanned(text.payload()))
        assertEquals(QrContentType.TEXT, (parseScanned("что угодно") as QrContent.Text).kind)
    }

    @Test
    fun `стиль кодируется и декодируется без потерь`() {
        val style = QrStyle(
            dotStyle = com.kizqr.app.qr.DotStyle.DOTS,
            eyeStyle = com.kizqr.app.qr.EyeStyle.LEAF,
            colorMode = com.kizqr.app.qr.QrColorMode.LINEAR,
            color1 = 0xFF229ED9.toInt(),
            color2 = 0xFFDD2A7B.toInt(),
            eyeColor = 0xFF00B8A9.toInt(),
            bgColor = 0xFFF0F0F0.toInt(),
            transparentBg = true,
            logo = com.kizqr.app.qr.LogoPreset.TELEGRAM,
            logoUri = null,
            errorCorrection = com.kizqr.app.qr.QrErrorCorrection.Q,
            quietZone = false,
        )
        assertEquals(style, QrStyle.decode(style.encode()))
    }
}

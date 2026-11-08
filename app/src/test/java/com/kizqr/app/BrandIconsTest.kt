package com.kizqr.app

import com.kizqr.app.qr.BrandIcons
import com.kizqr.app.qr.BrandIconPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandIconsTest {

    private fun normalize(vararg tokens: String): List<String> =
        BrandIcons.normalize(tokens.joinToString(" ")).trim().split(Regex("\\s+"))

    @Test
    fun `слитные arc-флаги разделяются`() {
        val tokens = normalize("a9.87 9.87 0 01-5.031-1.378")
        assertEquals(
            // rot=0, затем слитые флаги 0 и 1, потом координаты
            listOf("a", "9.87", "9.87", "0", "0", "1", "-5.031", "-1.378"),
            tokens,
        )
    }

    @Test
    fun `слитные числа разделяются`() {
        val tokens = normalize("m9.489.004.729-.003h3.564")
        assertEquals(
            listOf("m", "9.489", ".004", ".729", "-.003", "h", "3.564"),
            tokens,
        )
    }

    @Test
    fun `arc с флагами и повторами параметров`() {
        val tokens = normalize("A11.815 11.815 0 0012.05 0C5.495 0.16 5.335.157 11.892c0 2.096")
        // A: rx ry rot flag flag x | C: ... | c: ...
        assertEquals(
            listOf(
                "A", "11.815", "11.815", "0", "0", "0", "12.05", "0",
                "C", "5.495", "0.16", "5.335", ".157", "11.892",
                "c", "0", "2.096",
            ),
            tokens,
        )
    }

    @Test
    fun `пробелы и запятые игнорируются`() {
        assertEquals(
            normalize("M 1 , 2 L3,4"),
            normalize("M1 2 L 3 4"),
        )
    }

    @Test
    fun `экспонента читается как часть числа`() {
        val tokens = normalize("M1e2 2.5E-3L4 5")
        assertEquals(listOf("M", "1e2", "2.5E-3", "L", "4", "5"), tokens)
    }

    @Test
    fun `все пути брендов нормализуются`() {
        for (path in listOf(
            BrandIconPaths.TELEGRAM,
            BrandIconPaths.WHATSAPP,
            BrandIconPaths.VK,
            BrandIconPaths.INSTAGRAM,
            BrandIconPaths.YOUTUBE,
            BrandIconPaths.SPOTIFY,
        )) {
            val result = BrandIcons.normalize(path)
            assertTrue(result.isNotBlank())
            // после нормализации нет слитых флагов/чисел: все токены корректны
            result.trim().split(Regex("\\s+")).forEach { token ->
                assertTrue("Bad token: $token", token.first().isLetter() || token.first().isDigit() || token.first() in "+-.")
            }
        }
    }
}

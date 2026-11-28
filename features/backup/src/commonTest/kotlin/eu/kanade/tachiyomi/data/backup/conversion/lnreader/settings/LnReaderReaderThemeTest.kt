package eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LnReaderReaderThemeTest {

    @Test
    fun `a CSS text colour's trailing alpha dims it over the background`() {
        // LNReader's black preset; read as Android's #AARRGGBB it would turn the text a saturated colour
        LnReaderReaderColors("#000000", "#FFFFFFB3").toKatariTheme()?.id shouldBe "CUSTOM:000000:B3B3B3"
        LnReaderReaderColors("#f5f5fa", "#111").toKatariTheme()?.id shouldBe "CUSTOM:F5F5FA:111111"
    }
}

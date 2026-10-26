package mihon.entry.interactions.reader.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BookDocumentReaderThemeTest {
    @Test
    fun `themes stored before custom colours, and saved colours, read back after an update`() {
        // The theme preference and per-book overrides stored the old enum's names.
        listOf("APP", "PAPER", "DUSK", "BLACK").map(BookDocumentReaderTheme::fromId) shouldBe listOf(
            BookDocumentReaderTheme.App,
            BookDocumentReaderTheme.Paper,
            BookDocumentReaderTheme.Dusk,
            BookDocumentReaderTheme.Black,
        )
        BookDocumentReaderTheme.fromId("CUSTOM:F5EEDC:2B2721") shouldBe
            BookDocumentReaderTheme.Custom(background = 0xFFF5EEDC.toInt(), text = 0xFF2B2721.toInt())
        BookDocumentReaderTheme.decodeCustomList(
            "CUSTOM:000000:E6E6E6;CUSTOM:nothex:000000;CUSTOM:FFFFFF:000000",
        ) shouldBe
            listOf(
                BookDocumentReaderTheme.Custom(background = 0xFF000000.toInt(), text = 0xFFE6E6E6.toInt()),
                BookDocumentReaderTheme.Custom(background = 0xFFFFFFFF.toInt(), text = 0xFF000000.toInt()),
            )
    }
}

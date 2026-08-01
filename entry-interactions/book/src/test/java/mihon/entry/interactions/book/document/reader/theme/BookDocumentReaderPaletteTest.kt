package mihon.entry.interactions.book.document.reader.theme

import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BookDocumentReaderPaletteTest {
    @Test
    fun `app mode follows app colors and black mode has a fixed pure black palette`() {
        val app = resolveBookDocumentReaderPalette(
            BookDocumentReaderThemeMode.APP,
            APP_BACKGROUND,
            APP_FOREGROUND,
        )
        val black = resolveBookDocumentReaderPalette(
            BookDocumentReaderThemeMode.BLACK,
            APP_BACKGROUND,
            APP_FOREGROUND,
        )

        assertEquals(APP_BACKGROUND, app.backgroundArgb)
        assertEquals(APP_FOREGROUND, app.foregroundArgb)
        assertEquals(0xFF000000, black.backgroundArgb)
        assertEquals(0xFFE6E6E6, black.foregroundArgb)
    }

    private companion object {
        const val APP_BACKGROUND = 0xFF010203L
        const val APP_FOREGROUND = 0xFFF1F2F3L
    }
}

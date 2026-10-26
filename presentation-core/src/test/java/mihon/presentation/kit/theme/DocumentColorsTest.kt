package mihon.presentation.kit.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DocumentColorsTest {
    @Test
    fun `authored text stays readable on every theme, also inside an authored box, and is unchanged on a white page`() {
        val white = ReadingPalette(Color.White, Color.Black, Color.Blue, Color.Red)
        val palettes = listOf(white, ReadingPalette.Paper, ReadingPalette.Dusk, BLACK)
        // Black and grey body text, a game window's white text on navy, and the pale yellow sites use for emphasis.
        val texts = listOf(0xFF000000L, 0xFF555555L, 0xFFFFFFFFL, 0xFFFFF59DL, 0xFF1E88E5L)
        val boxes = listOf(null, 0xFF102030L, 0xFFF1F1F1L)
        palettes.forEach { palette ->
            boxes.forEach { box ->
                val backdrop = box?.let(palette::documentColor) ?: palette.background
                texts.forEach { text ->
                    palette.documentTextColor(
                        text,
                        backdrop,
                    ).compositeOver(backdrop).contrastWith(backdrop) shouldBeGreaterThanOrEqual
                        3f
                }
            }
        }
        white.documentColor(0xFF1E88E5L).toArgb() shouldBe Color(0xFF1E88E5L).toArgb()
        white.documentTextColor(0xFF000000L).toArgb() shouldBe Color.Black.toArgb()
    }
}

private val BLACK = ReadingPalette(Color.Black, Color(0xFFE6E6E6), Color(0xFFE6E6E6), Color.Red)

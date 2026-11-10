package app.katari.entry.interactions.manga.reader.image.background

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class SideBackdropChoiceTest {
    @Test
    fun `dark artwork between a black gap and a white one stays black beside it and fades to white at its end`() {
        val page = PagePixels(WIDTH, 1000) { x, y ->
            when {
                y < 400 -> BLACK
                y >= 600 -> WHITE
                // Between the two, a dark panel reaching both edges, with artwork in its middle
                x < 20 || x >= WIDTH - 20 -> NAVY
                else -> if ((x + y) % 2 == 0) BLACK else WHITE
            }
        }

        chooseSideBackdrop(page) shouldContainExactly listOf(
            BackdropSpan(0f, 0.58f, BLACK, BLACK),
            BackdropSpan(0.58f, 0.6f, BLACK, WHITE),
            BackdropSpan(0.6f, 1f, WHITE, WHITE),
        )
    }

    @Test
    fun `a gradient across the whole width is no gap, so the sides keep the gaps around it`() {
        val page = PagePixels(WIDTH, 1000) { _, y ->
            if (y in 400..<600) gray(150 + (y - 400) / 2) else WHITE
        }

        chooseSideBackdrop(page) shouldContainExactly listOf(BackdropSpan(0f, 1f, WHITE, WHITE))
    }

    @Test
    fun `a saturated band across the whole width is artwork, so the sides keep the gaps around it`() {
        val page = PagePixels(WIDTH, 1000) { _, y -> if (y in 400..<600) BLUE else WHITE }

        chooseSideBackdrop(page) shouldContainExactly listOf(BackdropSpan(0f, 1f, WHITE, WHITE))
    }

    private fun gray(level: Int): Int = (0xFF shl 24) or (level shl 16) or (level shl 8) or level
}

private const val WIDTH = 200
private const val BLACK = 0xFF000000.toInt()
private const val NAVY = 0xFF14192D.toInt()
private const val BLUE = 0xFF3C96D2.toInt()

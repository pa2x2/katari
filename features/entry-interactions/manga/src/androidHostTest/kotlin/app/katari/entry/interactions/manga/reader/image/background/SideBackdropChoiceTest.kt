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

        chooseSideBackdrop(page.rowProfile()) shouldContainExactly listOf(
            BackdropSpan(0f, 0.58f, BLACK, BLACK),
            BackdropSpan(0.58f, 0.6f, BLACK, WHITE),
            BackdropSpan(0.6f, 1f, WHITE, WHITE),
        )
    }

    @Test
    fun `a gap takes the colour of most of its rows, not of the antialiased row where it begins`() {
        val page = PagePixels(WIDTH, 1000) { _, y ->
            when {
                y < 400 -> BLACK
                y == 400 -> FRINGE
                else -> WHITE
            }
        }

        chooseSideBackdrop(page.rowProfile()) shouldContainExactly listOf(
            BackdropSpan(0f, 0.4f, BLACK, BLACK),
            BackdropSpan(0.4f, 1f, WHITE, WHITE),
        )
    }

    @Test
    fun `a gradient across the whole width is no gap, so the sides keep the gaps around it`() {
        val page = PagePixels(WIDTH, 1000) { _, y ->
            if (y in 400..<600) gray(150 + (y - 400) / 2) else WHITE
        }

        chooseSideBackdrop(page.rowProfile()) shouldContainExactly listOf(BackdropSpan(0f, 1f, WHITE, WHITE))
    }

    @Test
    fun `a saturated band across the whole width is artwork, so the sides keep the gaps around it`() {
        val page = PagePixels(WIDTH, 1000) { _, y -> if (y in 400..<600) BLUE else WHITE }

        chooseSideBackdrop(page.rowProfile()) shouldContainExactly listOf(BackdropSpan(0f, 1f, WHITE, WHITE))
    }

    @Test
    fun `a page whose artwork starts above its first gap follows the page above it`() {
        val above = PagePixels(WIDTH, 400) { _, _ -> WHITE }
        val page = PagePixels(WIDTH, 1000) { x, y ->
            when {
                // A black caption on white across the top, then a dark panel, then a black gap
                y < 100 -> if (x in 50..<120) BLACK else WHITE
                y < 400 -> if (x < 20 || x >= WIDTH - 20) {
                    NAVY
                } else if ((x + y) % 2 == 0) {
                    BLACK
                } else {
                    WHITE
                }
                else -> BLACK
            }
        }

        val joined = listOf(above.rowProfile(), page.rowProfile()).joined(WIDTH)

        // White beside the page above and the caption's rows, fading to black where the dark panel begins
        chooseSideBackdrop(joined) shouldContainExactly listOf(
            BackdropSpan(0f, 490f / 1400, WHITE, WHITE),
            BackdropSpan(490f / 1400, 510f / 1400, WHITE, BLACK),
            BackdropSpan(510f / 1400, 1f, BLACK, BLACK),
        )
    }

    private fun gray(level: Int): Int = (0xFF shl 24) or (level shl 16) or (level shl 8) or level
}

private const val WIDTH = 200
private const val BLACK = 0xFF000000.toInt()
private const val NAVY = 0xFF14192D.toInt()
private const val BLUE = 0xFF3C96D2.toInt()
private const val FRINGE = 0xFFFAFCFD.toInt()

package app.katari.entry.interactions.manga.reader.image.background

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class SideBackdropChoiceTest {
    @Test
    fun `a page that turns from black to white is continued black beside its top and white beside its bottom`() {
        val page = PagePixels(WIDTH, 1000) { x, y ->
            when {
                y < 400 -> BLACK
                // A panel reaching both edges between the two
                y < 600 -> if ((x + y) % 2 == 0) BLACK else WHITE
                else -> WHITE
            }
        }

        val backdrop = chooseSideBackdrop(page)

        for (side in listOf(backdrop.left, backdrop.right)) {
            side shouldContainExactly listOf(
                BackdropSpan(0f, 0.4f, BLACK, BLACK),
                BackdropSpan(0.4f, 0.6f, BLACK, WHITE),
                BackdropSpan(0.6f, 1f, WHITE, WHITE),
            )
        }
    }

    @Test
    fun `a gradient across the whole width is no gap, so the sides keep the gaps around it`() {
        val page = PagePixels(WIDTH, 1000) { _, y ->
            if (y in 400..<600) gray(150 + (y - 400) / 2) else WHITE
        }

        val backdrop = chooseSideBackdrop(page)

        backdrop.left.map { it.topArgb to it.bottomArgb }.distinct() shouldContainExactly listOf(WHITE to WHITE)
        backdrop.right.map { it.topArgb to it.bottomArgb }.distinct() shouldContainExactly listOf(WHITE to WHITE)
    }

    private fun gray(level: Int): Int = (0xFF shl 24) or (level shl 16) or (level shl 8) or level
}

private const val WIDTH = 200
private const val BLACK = 0xFF000000.toInt()

package app.katari.entry.interactions.manga.reader.image.background

import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test
import kotlin.random.Random

// A page decodes only the probe areas, so a pixel the choice reads outside them would come from nowhere
class PageBackgroundProbesTest {
    @Test
    fun `the choice reads only pixels inside the probe areas`() {
        for (pattern in PATTERNS) {
            for ((width, height) in SIZES) {
                val areas = PageBackgroundProbes(width, height).areas
                val outside = mutableListOf<Pair<Int, Int>>()
                val random = Random(width * 31 + height)
                val page = PagePixels(width, height) { x, y ->
                    if (areas.none { x in it.left..<it.right && y in it.top..<it.bottom }) outside += x to y
                    pixel(pattern, x, y, width, height, random)
                }

                choosePageBackground(page, besidePage = false)
                choosePageBackground(page, besidePage = true)

                outside.shouldBeEmpty()
            }
        }
    }

    private fun pixel(pattern: String, x: Int, y: Int, width: Int, height: Int, random: Random): Int = when (pattern) {
        "white" -> WHITE
        "black" -> BLACK
        "frame" -> if (x < width / 20 || x >= width - width / 20) BLACK else WHITE
        "darkTop" -> if (y < height / 3) BLACK else WHITE
        else -> if (random.nextBoolean()) BLACK else WHITE
    }
}

private val PATTERNS = listOf("white", "black", "frame", "darkTop", "noise")

private val SIZES = listOf(50 to 50, 100 to 140, 101 to 77, 720 to 1100, 800 to 12000, 1600 to 1100)

private const val BLACK = 0xFF000000.toInt()

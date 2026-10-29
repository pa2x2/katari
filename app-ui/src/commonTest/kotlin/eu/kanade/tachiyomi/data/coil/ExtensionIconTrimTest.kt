package eu.kanade.tachiyomi.data.coil

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExtensionIconTrimTest {

    @Test
    fun `the trimmed square stays inside the icon, ignores its faint shadow and keeps a wide icon whole`() {
        // Content near the top right corner, with a shadow pixel too faint to count in the opposite corner
        val nearCorner = icon(size = 8) { x, y ->
            when {
                x in 5..7 && y in 1..2 -> OPAQUE
                x == 0 && y == 7 -> SHADOW
                else -> TRANSPARENT
            }
        }
        nearCorner.trimmedSquare() shouldBe IconSquare(x = 5, y = 0, side = 3)

        icon(size = 8) { _, _ -> TRANSPARENT }.trimmedSquare() shouldBe null

        // As wide as the icon: a square around it is the whole icon, so nothing is cut
        icon(size = 8) { _, y -> if (y in 3..4) OPAQUE else TRANSPARENT }.trimmedSquare() shouldBe null
    }

    private fun icon(size: Int, argb: (x: Int, y: Int) -> Int): IconPixels =
        IconPixels(size, size, IntArray(size * size) { argb(it % size, it / size) })

    private companion object {
        const val OPAQUE = 0xFF336699.toInt()
        const val SHADOW = 0x1E000000
        const val TRANSPARENT = 0
    }
}

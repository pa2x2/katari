package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class ImageTilingTest {

    @Test
    fun `a webtoon strip is split into overlapping tiles that cover it completely`() {
        val strip = ImageRect(0, 0, 900, 4000)

        val tiles = planTiles(strip)

        tiles.first().top shouldBe 0
        tiles.last().bottom shouldBe 4000
        tiles.forEach { tile ->
            tile.width shouldBe 900
            tile.height shouldBeLessThanOrEqual 1350
        }
        tiles.zipWithNext().forEach { (upper, lower) ->
            // Every region shorter than the overlap is whole in at least one tile.
            (upper.bottom - lower.top) shouldBeGreaterThanOrEqual 1350 / 4
        }
    }
}

package mihon.text.recognition.runtime.geometry

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import org.junit.jupiter.api.Test

class ImageTilingTest {

    @Test
    fun `a page that is not much taller than wide is read as one tile`() {
        val page = ImageRect(0, 0, 1200, 1800)

        planTiles(page) shouldContainExactly listOf(page)
    }

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

    @Test
    fun `a narrow area is not cut into tiles shorter than a component reads at full resolution`() {
        val line = ImageRect(0, 0, 600, 100)

        planTiles(line, minimumLength = 640) shouldContainExactly listOf(line)
    }

    @Test
    fun `subsampling keeps the detector input edge but never upsamples small images`() {
        sampleSizeForMinimumEdge(ImageRect(0, 0, 2600, 3900), 640) shouldBe 4
        sampleSizeForMinimumEdge(ImageRect(0, 0, 1200, 1800), 640) shouldBe 1
        sampleSizeForMinimumEdge(ImageRect(0, 0, 300, 300), 640) shouldBe 1
    }
}

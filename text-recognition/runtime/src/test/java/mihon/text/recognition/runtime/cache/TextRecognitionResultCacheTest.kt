package mihon.text.recognition.runtime.cache

import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TextRecognitionResultCacheTest {

    @TempDir
    lateinit var directory: File

    private var now = 1_000_000L
    private val regions = listOf(
        RecognizedTextRegion(
            bounds = ImageRect(10, 20, 110, 220),
            text = "素直にあやまるしか",
            kind = TextRegionKind.SpeechBubble,
            orientation = TextOrientation.Vertical,
            container = ImageRect(0, 0, 130, 240),
        ),
    )

    @Test
    fun `stored results survive a new cache instance`() {
        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).write(key("page"), regions)

        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).read(key("page")) shouldBe regions
    }

    @Test
    fun `exceeding the size limit evicts the least recently used results`() {
        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).write(key("probe"), regions)
        val probe = directory.listFiles()!!.single()
        val entrySize = probe.length()
        probe.delete()
        val bounded = TextRecognitionResultCache({ directory }, maximumBytes = entrySize * 2, clock = ::tick)
        bounded.write(key("first"), regions)
        bounded.write(key("second"), regions)
        bounded.read(key("first"))

        bounded.write(key("third"), regions)

        bounded.read(key("second")) shouldBe null
        bounded.read(key("first")) shouldBe regions
        bounded.read(key("third")) shouldBe regions
    }

    @Test
    fun `a corrupt entry reads as absent`() {
        val cache = TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000)
        cache.write(key("page"), regions)
        directory.listFiles()!!.single().writeText("{not json")

        cache.read(key("page")) shouldBe null
    }

    private fun key(name: String) = TextRecognitionCacheKey(listOf(name))

    /** Advances in whole seconds so file-system timestamp resolution cannot reorder entries. */
    private fun tick(): Long {
        now += 10_000
        return now
    }
}

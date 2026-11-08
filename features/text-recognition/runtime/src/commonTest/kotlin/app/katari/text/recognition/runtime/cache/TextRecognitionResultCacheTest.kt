package app.katari.text.recognition.runtime.cache

import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.result.RecognizedTextRegion
import app.katari.text.recognition.api.result.TextOrientation
import app.katari.text.recognition.api.result.TextRegionKind
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TextRecognitionResultCacheTest {

    @TempDir
    lateinit var directory: File

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
    fun `stored results survive a new cache instance and a corrupt entry reads as absent`() {
        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).write(key("page"), regions)

        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).read(key("page")) shouldBe regions

        directory.listFiles { file -> file.name.startsWith(key("page").digest) }!!.single().writeText("{not json")

        TextRecognitionResultCache({ directory }, maximumBytes = 1_000_000).read(key("page")) shouldBe null
    }

    private fun key(name: String) = TextRecognitionCacheKey(listOf(name))
}

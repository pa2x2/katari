package mihon.text.recognition.provider.mlkit

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.spi.component.RecognizedCropText
import org.junit.jupiter.api.Test

class MlKitCropTextTest {

    @Test
    fun `vertical columns are read from right to left without spaces`() {
        val lines = listOf(
            MlKitLine("いる", left = 0, top = 0, right = 20, bottom = 80),
            MlKitLine("何を", left = 60, top = 0, right = 80, bottom = 80),
            MlKitLine("して", left = 30, top = 0, right = 50, bottom = 80),
        )

        assembleCropText(lines, spaced = false) shouldBe RecognizedCropText("何をしている", TextOrientation.Vertical)
    }

    @Test
    fun `horizontal lines keep ML Kit's order and are joined with spaces in spaced scripts`() {
        val lines = listOf(
            MlKitLine("WHAT ARE YOU ", left = 0, top = 0, right = 120, bottom = 20),
            MlKitLine("DOING?", left = 20, top = 24, right = 80, bottom = 44),
        )

        assembleCropText(lines, spaced = true) shouldBe
            RecognizedCropText("WHAT ARE YOU DOING?", TextOrientation.Horizontal)
    }

    @Test
    fun `a crop without readable lines has no text`() {
        assembleCropText(listOf(MlKitLine(" ", 0, 0, 10, 10)), spaced = true).shouldBeNull()
    }
}

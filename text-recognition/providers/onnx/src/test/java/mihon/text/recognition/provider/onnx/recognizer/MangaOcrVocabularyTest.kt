package mihon.text.recognition.provider.onnx.recognizer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MangaOcrVocabularyTest {

    private val vocabulary = MangaOcrVocabulary(
        listOf("[PAD]", "[UNK]", "[CLS]", "[SEP]", "[MASK]", "何", "逃", "げ", "て", "る", "…", "・", "!", "##よ", " "),
    )

    @Test
    fun `decoding follows the upstream post-processing`() {
        vocabulary.decode(listOf(5, 1, 6, 7, 14, 8, 9, 13, 12, 12)) shouldBe "何逃げてるよ!!"
        vocabulary.decode(listOf(5, 10, 11, 11, 11)) shouldBe "何......"
    }
}

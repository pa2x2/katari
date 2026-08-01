package mihon.entry.interactions.reader.preparation

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderChapterPreparationPolicyTest {
    @Test
    fun `preparation threshold is inclusive`() {
        ReaderChapterPreparationPolicy.shouldPrepare(enabled = true, progression = 0.749) shouldBe false
        ReaderChapterPreparationPolicy.shouldPrepare(enabled = true, progression = 0.75) shouldBe true
        ReaderChapterPreparationPolicy.shouldPrepare(enabled = true, progression = 1.0) shouldBe true
        ReaderChapterPreparationPolicy.shouldPrepare(enabled = false, progression = 1.0) shouldBe false
    }
}

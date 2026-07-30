package mihon.entry.interactions.reader.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BookReaderLayoutModeTest {

    @Test
    fun `serialized values resolve and invalid legacy state falls back to pagination`() {
        BookReaderLayoutMode.fromSerializedValue("paginated") shouldBe BookReaderLayoutMode.PAGINATED
        BookReaderLayoutMode.fromSerializedValue("scrolling") shouldBe BookReaderLayoutMode.SCROLLING
        BookReaderLayoutMode.fromSerializedValue("legacy") shouldBe BookReaderLayoutMode.PAGINATED
        BookReaderLayoutMode.fromSerializedValue(null) shouldBe BookReaderLayoutMode.PAGINATED
    }
}

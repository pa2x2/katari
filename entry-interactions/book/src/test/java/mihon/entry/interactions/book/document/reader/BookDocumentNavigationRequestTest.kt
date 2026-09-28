package mihon.entry.interactions.book.document.reader

import io.kotest.matchers.shouldBe
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentPosition
import org.junit.jupiter.api.Test

class BookDocumentNavigationRequestTest {
    @Test
    fun `pending navigation rejects the old viewport and passage seeks wait for their own restoration`() {
        val request = request(id = 1, chapterId = 20)

        request.acceptsLocation(chapterId = 10, position = position(0)) shouldBe false
        request.acceptsLocation(chapterId = 20, position = position(10)) shouldBe false
        request.acceptsLocation(chapterId = 20, position = position(0)) shouldBe true

        val seek = request(id = 3, chapterId = 20).copy(alignToPassage = true)
        seek.acceptsLocation(20, position(0)) shouldBe false
        seek.acceptsLocation(20, position(0), restoredNavigationId = 2) shouldBe false
        seek.acceptsLocation(20, position(0), restoredNavigationId = 3) shouldBe true
    }

    @Test
    fun `target viewport consumes only the request that it observed`() {
        val observed = request(id = 1, chapterId = 20)
        val newer = request(id = 2, chapterId = 20)

        observed.afterAcceptedLocation(observed, chapterId = 20) shouldBe null
        newer.afterAcceptedLocation(observed, chapterId = 20) shouldBe newer
    }

    private fun request(id: Long, chapterId: Long) = BookDocumentNavigationRequest(
        id = id,
        chapterId = chapterId,
        position = position(0),
    )

    private fun position(offset: Int) = BookDocumentPosition(BookDocumentBlockId("block"), offset)
}

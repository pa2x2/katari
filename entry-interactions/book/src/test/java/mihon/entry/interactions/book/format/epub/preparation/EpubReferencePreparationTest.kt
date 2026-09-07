package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EpubReferencePreparationTest {
    @Test
    fun `supplementary Unicode manifest resource name reaches its prepared navigation target`() = runTest {
        val file = encodedEpubPublicationFile(chapterName = "章😀.xhtml", fragment = "位置😀")
        try {
            val result = EpubBookPreparer().prepare(EpubContentSessionFixture(file))
            val prepared = assertIs<BookPreparationResult.Success>(result).publication
            try {
                val publication = assertIs<PreparedBookDocumentPublication>(prepared)
                val document = publication.documents.single()
                val navigation = publication.publication.navigation.single()
                assertEquals("OPS/章😀.xhtml", document.resourceId)
                assertEquals(document.resourceId, navigation.target.resourceId)
                assertEquals(listOf("位置😀"), navigation.target.fragments)
                assertEquals(document.blocks.single().id, document.anchors.getValue("位置😀").blockId)
            } finally {
                prepared.close()
            }
        } finally {
            file.delete()
        }
    }
}

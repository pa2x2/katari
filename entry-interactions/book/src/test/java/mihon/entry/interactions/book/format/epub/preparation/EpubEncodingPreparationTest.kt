package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EpubEncodingPreparationTest {
    @Test
    fun `XML encodings preserve publication text titles navigation and linked stylesheet discovery`() = runTest {
        listOf(Charsets.UTF_8, Charsets.UTF_16LE, Charsets.UTF_16BE).forEach { charset ->
            val file = encodedEpubPublicationFile(xmlCharset = charset)
            try {
                val result = EpubBookPreparer().prepare(EpubContentSessionFixture(file))
                val prepared = assertIs<BookPreparationResult.Success>(result, charset.name()).publication
                try {
                    val publication = assertIs<PreparedBookDocumentPublication>(prepared)
                    assertEquals("Chapitre été", publication.publication.readingOrder.single().title)
                    assertEquals("été 日本語", publication.documents.single().blocks.single().plainText)
                    assertEquals(0xFF123456L, publication.documents.single().blocks.single().style.foregroundArgb)
                    val navigation = publication.publication.navigation.single()
                    assertEquals("Été", navigation.title)
                    assertEquals(listOf("position"), navigation.target.fragments)
                } finally {
                    prepared.close()
                }
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun `stylesheet byte order mark takes precedence over charset declaration`() = runTest {
        listOf(Charsets.UTF_16LE, Charsets.UTF_16BE).forEach { charset ->
            val file = encodedEpubPublicationFile(cssCharset = charset)
            try {
                val result = EpubBookPreparer().prepare(EpubContentSessionFixture(file))
                val prepared = assertIs<BookPreparationResult.Success>(result, charset.name()).publication
                try {
                    val publication = assertIs<PreparedBookDocumentPublication>(prepared)
                    assertEquals(0xFF123456L, publication.documents.single().blocks.single().style.foregroundArgb)
                } finally {
                    prepared.close()
                }
            } finally {
                file.delete()
            }
        }
    }
}

package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.BookResource
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadManifest
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import mihon.entry.interactions.book.download.BookDownloadedResource
import mihon.entry.interactions.book.download.VerifiedBookDownloadPackage
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.SourceManager
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookReaderDownloadedResolutionTest {
    @Test
    fun `downloaded child opens for a merged entry without resolving its unavailable source`() = runTest {
        val owner = entry(id = 1L, source = 9L, url = "/book")
        val visible = entry(id = 2L, source = 20L, url = "/merged")
        val chapter = EntryChapter.create().copy(
            id = 10L,
            entryId = owner.id,
            url = "/chapter/1",
            name = "Chapter 1",
        )
        val bytes = "<h1>Offline chapter</h1>".encodeToByteArray()
        val downloaded = downloadedPackage(owner, chapter, bytes)
        val packageKey = BookDownloadPackageKey(owner.source, owner.url, chapter.url)
        val downloadCache = mockk<BookDownloadCache>()
        coEvery { downloadCache.getVerified(packageKey) } returns downloaded
        val sourceManager = mockk<SourceManager>()
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { get(owner.id, "volume-1", "chapter") } returns null
        }
        val processor = DownloadedContentRecordingProcessor()
        val factory = BookReaderSessionFactory(
            entryRepository = mockk<EntryRepository> {
                coEvery { getEntryById(visible.id) } returns visible
                coEvery { getEntryById(owner.id) } returns owner
            },
            entryChapterRepository = mockk<EntryChapterRepository> {
                coEvery { getChapterById(chapter.id) } returns chapter
            },
            entryProgressRepository = progressRepository,
            historyRepository = mockk<HistoryRepository>(relaxed = true),
            sourceManager = sourceManager,
            processorRegistry = BookProcessorRegistry(listOf(processor)),
            networkHelper = mockk<NetworkHelper> {
                every { client } returns mockk<OkHttpClient>()
            },
            incognitoState = mockk(relaxed = true),
            materializationStore = mockk(relaxed = true),
            downloadCache = downloadCache,
        )

        val prepared = assertIs<BookReaderPrepareResult.Success>(
            factory.prepare(BookReaderRequest(visible.id, chapter.id)),
        ).request
        assertIs<PreparedBookContent.Downloaded>(prepared.content)
        val opened = assertIs<BookReaderOpenResult.Success>(
            factory.openPrepared(mockk<Context>(relaxed = true), prepared, processor.id),
        ).session

        assertEquals(visible, opened.entry)
        assertEquals(bytes.decodeToString(), processor.openedContent)
        verify(exactly = 0) { sourceManager.get(any()) }
        coVerify(exactly = 1) { progressRepository.get(owner.id, "volume-1", "chapter") }
        opened.close()
    }

    private fun entry(id: Long, source: Long, url: String): Entry = Entry.create().copy(
        id = id,
        source = source,
        url = url,
        title = "Book $id",
        type = EntryType.BOOK,
    )

    private fun downloadedPackage(
        owner: Entry,
        chapter: EntryChapter,
        bytes: ByteArray,
    ): VerifiedBookDownloadPackage {
        val resourceFile = mockk<UniFile> {
            every { openInputStream() } answers { ByteArrayInputStream(bytes) }
        }
        val manifest = BookDownloadManifest(
            sourceId = owner.source,
            entryId = owner.id,
            entryTitle = owner.title,
            entryUrl = owner.url,
            childId = chapter.id,
            childTitle = chapter.name,
            childUrl = chapter.url,
            descriptor = BookContentDescriptor("text/html", profile = "prose-chapter"),
            publicationId = "source:${owner.source}:entry:${owner.url}:publication:volume-1",
            publicationRevision = "publication-v1",
            catalogCoverage = BookCatalogCoverage.PARTIAL,
            primaryResourceIds = listOf("chapter"),
            progressContentKey = "volume-1",
            progressResourceId = "chapter",
            progressResourceRevision = "chapter-v1",
            resources = listOf(
                BookDownloadedResource(
                    id = "chapter",
                    mediaType = "text/html",
                    revision = "chapter-v1",
                    fileName = "chapter.html",
                    storedSize = bytes.size.toLong(),
                    sha256 = "0".repeat(64),
                ),
            ),
            createdAt = 1L,
        )
        return VerifiedBookDownloadPackage(
            directory = mockk(relaxed = true),
            manifest = manifest,
            resources = mapOf("chapter" to resourceFile),
        )
    }
}

private class DownloadedContentRecordingProcessor : BookProcessor {
    override val id = "test.downloaded.prose"
    override val displayName = "Downloaded prose test"
    var openedContent: String? = null

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == "text/html" && descriptor.profile == "prose-chapter"

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = Intent()

    override suspend fun open(content: BookContentSession): BookOpenResult {
        val resourceId = content.primaryResourceIds.single()
        openedContent = content.openResource(resourceId).getOrThrow().use { it.stream.reader().readText() }
        return BookOpenResult.Success(
            object : BookPublicationSession {
                override val publication = BookPublication(
                    id = content.publicationId,
                    revision = content.revision,
                    title = "Offline chapter",
                    languages = emptyList(),
                    readingDirection = null,
                    readingOrder = listOf(BookResource(resourceId, "text/html", "Chapter")),
                    navigation = emptyList(),
                )

                override fun validate(locator: BookLocator): Boolean = locator.resourceId == resourceId

                override fun close() = Unit
            },
        )
    }
}

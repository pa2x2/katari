package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.BookMaterializationCache
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.preparation.BookContentPreparer
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.download.EntryDownloadPhase
import mihon.entry.interactions.download.EntryDownloadProgress
import mihon.entry.interactions.download.EntryDownloadQueueItem
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDownloadTransferProgressTest {
    @Test
    fun `network bytes advance displayed progress before preparation and completion`() = runTest {
        val snapshots = downloadWithObservedTransfer(knownLength = true)

        assertTrue(snapshots.duringTransfer.any { it.progress in 1..98 })
        assertTrue(
            snapshots.duringTransfer.all {
                it.presentation.phase == EntryDownloadPhase.TRANSFERRING && it.progress < 100
            },
        )
        assertTrue(snapshots.duringTransfer.any { it.presentation.progress is EntryDownloadProgress.Percent })
        assertEquals(EntryDownloadPhase.FINALIZING, snapshots.duringPreparation.presentation.phase)
        assertEquals(EntryDownloadProgress.None, snapshots.duringPreparation.presentation.progress)
        assertTrue(snapshots.duringPreparation.progress < 100)
        assertEquals(EntryDownloadPhase.COMPLETED, snapshots.completed.presentation.phase)
        assertEquals(100, snapshots.completed.progress)
    }

    @Test
    fun `unknown response length displays downloading without inventing a percentage`() = runTest {
        val snapshots = downloadWithObservedTransfer(knownLength = false)

        assertTrue(snapshots.duringTransfer.isNotEmpty())
        assertTrue(
            snapshots.duringTransfer.all {
                it.presentation.phase == EntryDownloadPhase.TRANSFERRING &&
                    it.presentation.progress == EntryDownloadProgress.None && it.progress == 0
            },
        )
        assertEquals(EntryDownloadPhase.FINALIZING, snapshots.duringPreparation.presentation.phase)
        assertEquals(EntryDownloadPhase.COMPLETED, snapshots.completed.presentation.phase)
    }

    private suspend fun downloadWithObservedTransfer(knownLength: Boolean): TransferSnapshots {
        val application = RuntimeEnvironment.getApplication()
        val fixture = fixture()
        val download = BookDownload(fixture.entry, fixture.child)
        val descriptor = BookContentDescriptor("text/html", profile = "prose")
        val content = "<p>${"Reading ".repeat(16384)}</p>"
        val bytes = content.encodeToByteArray()
        val duringTransfer = mutableListOf<EntryDownloadQueueItem>()
        val responseBody = object : ResponseBody() {
            private val input = object : ForwardingSource(Buffer().write(bytes)) {
                override fun read(sink: Buffer, byteCount: Long): Long {
                    duringTransfer += download.toEntryDownloadQueueItem()
                    return super.read(sink, minOf(byteCount, 8192L))
                }
            }.buffer()
            override fun contentType() = null
            override fun contentLength() = if (knownLength) bytes.size.toLong() else -1L
            override fun source() = input
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(responseBody).build()
        }.build()
        val source = mockk<UnifiedSource> {
            every { id } returns fixture.entry.source
            every { name } returns "Fixture"
            coEvery { getMedia(any(), any()) } returns EntryMedia.Book(
                descriptor = descriptor,
                catalog = BookResourceCatalog(
                    resources = listOf(
                        BookSourceResource(
                            id = "chapter",
                            mediaType = "text/html",
                            location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                        ),
                    ),
                ),
                initialResourceId = "chapter",
            )
        }
        var duringPreparation: EntryDownloadQueueItem? = null
        val delegate = ValidatingPreparer(descriptor, expectedContent = content)
        val preparer = object : BookContentPreparer by delegate {
            override suspend fun prepare(content: BookContentSession) = delegate.prepare(content).also {
                duringPreparation = download.toEntryDownloadQueueItem()
            }
        }
        val provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(fixture.root) })
        val downloader = BookDownloader(
            application = application,
            provider = provider,
            cache = BookDownloadCache(provider),
            sourceManager = mockk { every { get(fixture.entry.source) } returns source },
            networkHelper = mockk { every { this@mockk.client } returns client },
            materializationStore = BookMaterializationCache(
                application,
                Files.createTempDirectory("book-progress-materialization").toFile(),
            ),
            preparerRegistry = BookContentPreparerRegistry(listOf(preparer)),
        )
        assertNull(downloader.download(download))
        return TransferSnapshots(duringTransfer, checkNotNull(duringPreparation), download.toEntryDownloadQueueItem())
    }
}

private data class TransferSnapshots(
    val duringTransfer: List<EntryDownloadQueueItem>,
    val duringPreparation: EntryDownloadQueueItem,
    val completed: EntryDownloadQueueItem,
)

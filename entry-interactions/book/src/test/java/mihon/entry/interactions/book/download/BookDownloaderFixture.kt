package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookPublication
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.BookContentPreparer
import mihon.entry.interactions.book.BookContentPreparerRegistry
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookContentSessionResourceLoader
import mihon.entry.interactions.book.BookMaterializationCache
import mihon.entry.interactions.book.BookPreparationResult
import mihon.entry.interactions.book.BookPublicationResourceDependencies
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.PreparedBookPublication
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal abstract class BookDownloaderFixture {
    protected suspend fun downloadWithBudget(
        budget: BookDownloadResourceBudget,
        primaryBytes: ByteArray = "<p>Budgeted</p>".encodeToByteArray(),
        assetBytes: ByteArray = byteArrayOf(1, 2, 3, 4),
    ): BudgetDownloadResult {
        val application = RuntimeEnvironment.getApplication()
        val downloadRoot = Files.createTempDirectory("book-budget-download").toFile()
        val provider = BookDownloadProvider(
            downloadsDirectory = { UniFile.fromFile(downloadRoot) },
        )
        val cache = BookDownloadCache(provider)
        val entry = Entry.create().copy(
            id = 101L,
            profileId = 2L,
            source = 142L,
            url = "/books/budget",
            title = "Budget Book",
            type = EntryType.BOOK,
        )
        val chapter = EntryChapter.create().copy(
            id = 110L,
            entryId = entry.id,
            url = "/books/budget/chapter",
            name = "Budget Chapter",
        )
        val descriptor = BookContentDescriptor("text/html", profile = "prose")
        val media = EntryMedia.Book(
            descriptor = descriptor,
            catalog = BookResourceCatalog(
                resources = listOf(
                    BookSourceResource(
                        id = "chapter",
                        title = "Chapter",
                        mediaType = "text/html",
                        location = BookResourceLocation.InlineBytes(primaryBytes),
                    ),
                    BookSourceResource(
                        id = "asset",
                        title = "Asset",
                        mediaType = "application/octet-stream",
                        location = BookResourceLocation.InlineBytes(assetBytes),
                    ),
                ),
            ),
            initialResourceId = "chapter",
        )
        val source = mockk<UnifiedSource> {
            every { id } returns entry.source
            every { name } returns "Fixture"
            coEvery { getMedia(any(), any()) } returns media
        }
        val downloader = BookDownloader(
            application = application,
            provider = provider,
            cache = cache,
            sourceManager = mockk {
                every { get(entry.source) } returns source
            },
            networkHelper = mockk {
                every { client } returns OkHttpClient()
            },
            materializationStore = BookMaterializationCache(
                application,
                Files.createTempDirectory("book-budget-materialization").toFile(),
            ),
            preparerRegistry = BookContentPreparerRegistry(
                listOf(
                    ValidatingPreparer(
                        descriptor = descriptor,
                        expectedContent = primaryBytes.decodeToString(),
                        requiredResourceIds = setOf("asset"),
                    ),
                ),
            ),
            resourceBudget = budget,
        )

        val failure = downloader.download(BookDownload(entry, chapter))
        return BudgetDownloadResult(
            failure = failure,
            completedPackage = cache.get(BookDownloadPackageKey(entry.source, entry.url, chapter.url)),
        )
    }
}
internal data class BudgetDownloadResult(
    val failure: BookDownloadFailure?,
    val completedPackage: VerifiedBookDownloadPackage?,
)

internal class ValidatingPreparer(
    private val descriptor: BookContentDescriptor,
    private val expectedContent: String = "<p>Offline</p>",
    private val requiredResourceIds: Set<String> = emptySet(),
    private val resourceRequirements: Map<String, BookResourceRequirement> = emptyMap(),
) : BookContentPreparer {
    override val id = "validating"
    override val outputModel = ValidatingPublicationModel.descriptor

    override fun supports(descriptor: BookContentDescriptor): Boolean = descriptor == this.descriptor

    override suspend fun prepare(content: BookContentSession): BookPreparationResult {
        content.openResource("chapter").getOrThrow().use { resource ->
            check(resource.stream.reader().readText() == expectedContent)
        }
        return BookPreparationResult.Success(
            object : PreparedBookPublication, BookPublicationResourceDependencies {
                override val model = ValidatingPublicationModel
                override val resourceLoader = BookContentSessionResourceLoader(content)
                override val requiredResourceIds = this@ValidatingPreparer.requiredResourceIds
                override val resourceRequirements = this@ValidatingPreparer.resourceRequirements
                override val publication = BookPublication(
                    id = content.publicationId,
                    revision = content.revision,
                    title = "Test Book",
                    languages = emptyList(),
                    readingDirection = null,
                    readingOrder = emptyList(),
                    navigation = emptyList(),
                )

                override fun validate(locator: mihon.book.api.BookLocator) = true
                override fun close() = Unit
            },
        )
    }
}

private object ValidatingPublicationModel : BookPublicationModel {
    override val descriptor = BookPublicationModelDescriptor("test.validating")
}

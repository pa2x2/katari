package mihon.entry.interactions.book.download

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.content.AndroidBookExternalResourceResolver
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.content.BookResourceMaterializationLimitException
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.content.SourceBookContentSession
import mihon.entry.interactions.book.document.resource.validateBookDocumentResource
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.preparation.BookContentPreparerSelection
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.BookPublicationResourceDependencies
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import mihon.entry.interactions.book.state.progressIdentity
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.security.MessageDigest

internal class BookDownloader(
    private val application: Application = Injekt.get(),
    private val provider: BookDownloadProvider = Injekt.get(),
    private val cache: BookDownloadCache = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkHelper: NetworkHelper = Injekt.get(),
    private val materializationStore: BookMaterializationStore = Injekt.get(),
    private val preparerRegistry: BookContentPreparerRegistry = Injekt.get(),
    private val resourceBudget: BookDownloadResourceBudget = BookDownloadResourceBudget(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun download(download: BookDownload): BookDownloadFailure? {
        val source = sourceManager.get(download.entry.source)
            ?: return failure(BookDownloadFailure.Reason.SOURCE_NOT_FOUND)
        download.status = BookDownload.State.RESOLVING
        download.progress = 0

        val media = try {
            source.getMedia(download.chapter.toSEntryChapter()) as? EntryMedia.Book
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return failure(BookDownloadFailure.Reason.NETWORK, error.message)
        } ?: return failure(
            BookDownloadFailure.Reason.CONTENT_UNAVAILABLE,
            "The source did not return BOOK content.",
        )
        val primaryResourceId = media.initialResourceId ?: media.catalog.resources.singleOrNull()?.id
            ?: return failure(
                BookDownloadFailure.Reason.AMBIGUOUS_RESOURCE,
                "The BOOK item does not identify exactly one primary resource.",
            )
        val preparer = when (val selection = preparerRegistry.resolve(media.descriptor)) {
            BookContentPreparerSelection.Unsupported -> {
                return failure(BookDownloadFailure.Reason.UNSUPPORTED_FORMAT)
            }
            is BookContentPreparerSelection.Ambiguous -> {
                return failure(
                    BookDownloadFailure.Reason.UNSUPPORTED_FORMAT,
                    "Multiple BOOK preparers claim this content: ${selection.preparers.joinToString { it.id }}",
                )
            }
            is BookContentPreparerSelection.Selected -> selection.preparer
        }
        val session = SourceBookContentSession(
            source = source,
            entry = download.entry,
            media = media,
            externalResolver = AndroidBookExternalResourceResolver(
                context = application,
                httpClient = (source as? EntryHttpSource)?.client ?: networkHelper.client,
            ),
            materializationStore = materializationStore,
        )

        try {
            val primaryMaterialized = session.materializeResource(
                primaryResourceId,
                resourceBudget.maxEncodedBytes,
            ).getOrElse { error ->
                return failure(
                    if (error is BookResourceMaterializationLimitException) {
                        BookDownloadFailure.Reason.INTEGRITY
                    } else {
                        BookDownloadFailure.Reason.NETWORK
                    },
                    error.message,
                )
            }
            primaryMaterialized.use { primaryResource ->
                val processingContent = MaterializedPrimaryBookContentSession(
                    delegate = session,
                    primaryResourceId = primaryResourceId,
                    primaryResource = primaryResource,
                )
                val dependencies = when (val prepared = preparer.prepare(processingContent)) {
                    is BookPreparationResult.Failure -> return failure(
                        BookDownloadFailure.Reason.CONTENT_UNAVAILABLE,
                        prepared.failure.message,
                    )
                    is BookPreparationResult.Success -> try {
                        (prepared.publication as? BookPublicationResourceDependencies)?.let { resources ->
                            ResolvedBookResourceDependencies(
                                resourceIds = resources.requiredResourceIds,
                                requirements = resources.resourceRequirements,
                            )
                        } ?: ResolvedBookResourceDependencies()
                    } finally {
                        prepared.publication.close()
                    }
                }
                val resourceIds = (listOf(primaryResourceId) + dependencies.resourceIds)
                    .distinct()
                if (resourceIds.size > resourceBudget.maxResourceCount) {
                    return failure(
                        BookDownloadFailure.Reason.INTEGRITY,
                        "BOOK download requires ${resourceIds.size} resources; " +
                            "the limit is ${resourceBudget.maxResourceCount}.",
                    )
                }
                val staging = provider.beginPackage(source.name, download.entry, download.chapter).getOrElse { error ->
                    return failure(BookDownloadFailure.Reason.STORAGE, error.message)
                }
                val budgetTracker = resourceBudget.tracker()
                try {
                    download.status = BookDownload.State.DOWNLOADING

                    suspend fun packageResource(
                        resourceId: String,
                        resource: MaterializedBookResource,
                        index: Int,
                    ): BookDownloadedResource {
                        budgetTracker.include(resource.file.length(), resourceId)
                        dependencies.requirements[resourceId]?.let { requirement ->
                            try {
                                resource.file.validateBookDocumentResource(
                                    mediaType = resource.metadata.mediaType,
                                    requirement = requirement,
                                )
                            } catch (error: Exception) {
                                resource.invalidate()
                                throw BookResourceValidationException(
                                    "Required BOOK resource $resourceId is invalid: ${error.message}",
                                    error,
                                )
                            }
                        }
                        val fileName = provider.resourceFileName(resource.metadata.id, resource.metadata.mediaType)
                        val output = staging.directory.createFile(fileName)
                            ?: throw IOException("Unable to create downloaded BOOK resource")
                        val progressStart = index * 95 / resourceIds.size
                        val progressEnd = (index + 1) * 95 / resourceIds.size
                        val copied = copyResource(
                            source = resource.file,
                            target = output,
                            download = download,
                            progressStart = progressStart,
                            progressEnd = progressEnd,
                        )
                        return BookDownloadedResource(
                            id = resource.metadata.id,
                            title = resource.metadata.title,
                            order = resource.metadata.order,
                            groupId = resource.metadata.groupId,
                            mediaType = resource.metadata.mediaType,
                            revision = resource.metadata.revision,
                            fileName = fileName,
                            storedSize = copied.size,
                            sha256 = copied.sha256,
                        )
                    }

                    val downloadedResources = resourceIds.mapIndexed { index, resourceId ->
                        if (resourceId == primaryResourceId) {
                            packageResource(resourceId, primaryResource, index)
                        } else {
                            val metadata = session.getResource(resourceId).getOrElse { error ->
                                throw BookResourceDownloadException(error.message, error)
                            }
                            metadata.size?.let { declaredSize ->
                                budgetTracker.requireCanInclude(declaredSize, resourceId)
                            }
                            val acquisitionLimit = minOf(
                                dependencies.requirements[resourceId]?.maxBytes?.toLong() ?: Long.MAX_VALUE,
                                budgetTracker.remainingEncodedBytes,
                            )
                            if (acquisitionLimit <= 0L) {
                                throw BookResourceBudgetException(
                                    "BOOK resource $resourceId would exceed the download byte limit.",
                                )
                            }
                            session.materializeResource(resourceId, acquisitionLimit).getOrElse { error ->
                                if (error is BookResourceMaterializationLimitException) {
                                    throw BookResourceValidationException(error.message, error)
                                }
                                throw BookResourceDownloadException(error.message, error)
                            }.use { resource ->
                                packageResource(resourceId, resource, index)
                            }
                        }
                    }
                    val progressIdentity = media.progressIdentity(download.chapter.id)
                    val manifest = BookDownloadManifest(
                        sourceId = download.entry.source,
                        entryId = download.entry.id,
                        entryTitle = download.entry.title,
                        entryUrl = download.entry.url,
                        childId = download.chapter.id,
                        childTitle = download.chapter.name,
                        childUrl = download.chapter.url,
                        descriptor = session.descriptor,
                        publicationId = session.publicationId,
                        publicationRevision = session.revision,
                        languages = session.languages,
                        catalogRevision = session.catalogRevision,
                        catalogCoverage = session.catalogCoverage,
                        resourceHierarchy = session.resourceHierarchy,
                        primaryResourceIds = listOf(primaryResourceId),
                        progressContentKey = progressIdentity.contentKey,
                        progressResourceId = progressIdentity.resourceKey,
                        progressResourceRevision = progressIdentity.resourceRevision,
                        resources = downloadedResources,
                        createdAt = now(),
                    )
                    val published = provider.completePackage(staging, manifest).getOrElse { error ->
                        throw BookPackageIntegrityException(error.message, error)
                    }
                    cache.upsert(published)
                } catch (error: CancellationException) {
                    staging.directory.delete()
                    throw error
                } catch (error: Exception) {
                    staging.directory.delete()
                    return failure(
                        when (error) {
                            is BookPackageIntegrityException -> BookDownloadFailure.Reason.INTEGRITY
                            is BookResourceBudgetException -> BookDownloadFailure.Reason.INTEGRITY
                            is BookResourceValidationException -> BookDownloadFailure.Reason.INTEGRITY
                            is BookResourceDownloadException -> BookDownloadFailure.Reason.NETWORK
                            else -> BookDownloadFailure.Reason.STORAGE
                        },
                        error.message,
                    )
                }
                download.progress = 100
                download.status = BookDownload.State.DOWNLOADED
                return null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return failure(BookDownloadFailure.Reason.CONTENT_UNAVAILABLE, error.message)
        } finally {
            session.close()
        }
    }

    private suspend fun copyResource(
        source: java.io.File,
        target: com.hippo.unifile.UniFile,
        download: BookDownload,
        progressStart: Int,
        progressEnd: Int,
    ): CopiedResource = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        val expectedSize = source.length().coerceAtLeast(1L)
        var copied = 0L
        source.inputStream().buffered().use { input ->
            target.openOutputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    copied += read
                    val progressSpan = (progressEnd - progressStart).coerceAtLeast(0)
                    download.progress = (
                        progressStart + (copied * progressSpan / expectedSize).toInt()
                        ).coerceAtMost(progressEnd)
                }
            }
        }
        require(copied > 0L) { "Downloaded BOOK resource is empty" }
        CopiedResource(
            size = copied,
            sha256 = digest.digest().joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            },
        )
    }

    private fun failure(reason: BookDownloadFailure.Reason, message: String? = null) =
        BookDownloadFailure(reason, message)
}

private data class CopiedResource(val size: Long, val sha256: String)

private data class ResolvedBookResourceDependencies(
    val resourceIds: Set<String> = emptySet(),
    val requirements: Map<String, BookResourceRequirement> = emptyMap(),
)

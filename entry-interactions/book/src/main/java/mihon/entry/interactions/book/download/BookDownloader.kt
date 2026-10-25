package mihon.entry.interactions.book.download

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.content.AndroidBookExternalResourceResolver
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.content.BookResourceMaterializationLimitException
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.content.SourceBookContentSession
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.preparation.BookContentPreparerSelection
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.BookPublicationResourceDependencies
import mihon.entry.interactions.book.preparation.BookResourceContentKind
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import mihon.entry.interactions.book.state.progressIdentity
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized
import java.io.IOException

internal class BookDownloader(
    private val application: Application,
    private val provider: BookDownloadProvider,
    private val cache: BookDownloadCache,
    private val sourceManager: SourceManager,
    private val networkHelper: NetworkHelper,
    private val materializationStore: BookMaterializationStore,
    private val preparerRegistry: BookContentPreparerRegistry,
    private val resourceBudget: BookDownloadResourceBudget = BookDownloadResourceBudget(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    // Extensions read logins and API keys from the entry's profile's source preferences, not the active profile's.
    suspend fun download(download: BookDownload): BookDownloadFailure? =
        withContext(SourcePreferenceProfile.of(download.entry.profileId)) { downloadInProfile(download) }

    private suspend fun downloadInProfile(download: BookDownload): BookDownloadFailure? {
        // The restored queue resumes as the process starts, before the sources have loaded
        sourceManager.awaitInitialized()
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
            val primaryMaterialized = materializeForDownload(
                session,
                download,
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
                    val writer = BookDownloadResourceWriter(provider, staging, budgetTracker, dependencies.requirements)

                    val downloadedResources = resourceIds.mapNotNull { resourceId ->
                        if (resourceId == primaryResourceId) {
                            writer.write(resourceId, primaryResource)
                        } else {
                            try {
                                downloadSubordinate(session, download, resourceId, dependencies, budgetTracker, writer)
                            } catch (error: IOException) {
                                // A dead illustration or font must not cost the whole chapter; offline it reads as
                                // its alternative text or in the reader's font, as it does online. Transient failures
                                // still fail the download so it is retried.
                                val requirement = dependencies.requirements[resourceId]
                                if (requirement?.contentKind !in OPTIONAL_RESOURCE_KINDS ||
                                    !error.isPermanentResourceFailure()
                                ) {
                                    throw error
                                }
                                null
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
                        styleSheets = session.styleSheets,
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

    private suspend fun downloadSubordinate(
        session: SourceBookContentSession,
        download: BookDownload,
        resourceId: String,
        dependencies: ResolvedBookResourceDependencies,
        budgetTracker: BookDownloadResourceBudgetTracker,
        writer: BookDownloadResourceWriter,
    ): BookDownloadedResource {
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
        return materializeForDownload(session, download, resourceId, acquisitionLimit).getOrElse { error ->
            if (error is BookResourceMaterializationLimitException) {
                throw BookResourceValidationException(error.message, error)
            }
            throw BookResourceDownloadException(error.message, error)
        }.use { resource ->
            writer.write(resourceId, resource)
        }
    }

    private suspend fun materializeForDownload(
        session: SourceBookContentSession,
        download: BookDownload,
        resourceId: String,
        maxBytes: Long,
    ): Result<MaterializedBookResource> {
        download.progress = 0
        download.status = BookDownload.State.DOWNLOADING
        return session.materializeResource(resourceId, maxBytes) { bytesRead, totalBytes ->
            download.progress = if (totalBytes != null && totalBytes > 0L) {
                // Completion is only reported once the offline package passes verification.
                (bytesRead.toDouble() / totalBytes * 100).toInt().coerceIn(0, 99)
            } else {
                0
            }
        }.onSuccess {
            download.status = BookDownload.State.FINALIZING
        }
    }

    private fun failure(reason: BookDownloadFailure.Reason, message: String? = null) =
        BookDownloadFailure(reason, message)
}

private data class ResolvedBookResourceDependencies(
    val resourceIds: Set<String> = emptySet(),
    val requirements: Map<String, BookResourceRequirement> = emptyMap(),
)

private val OPTIONAL_RESOURCE_KINDS = setOf(BookResourceContentKind.DOCUMENT_IMAGE, BookResourceContentKind.FONT)

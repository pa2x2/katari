package mihon.entry.interactions.book.download

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.AndroidBookExternalResourceResolver
import mihon.entry.interactions.book.BookMaterializationStore
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.BookProcessorRegistry
import mihon.entry.interactions.book.SourceBookContentSession
import mihon.entry.interactions.book.download.model.BookDownload
import mihon.entry.interactions.book.download.model.BookDownloadFailure
import mihon.entry.interactions.book.progressIdentity
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
    private val processorRegistry: BookProcessorRegistry = Injekt.get(),
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
        val processor = processorRegistry.compatibleProcessors(media.descriptor)
            .sortedBy { it.id }
            .firstOrNull()
            ?: return failure(BookDownloadFailure.Reason.UNSUPPORTED_FORMAT)
        val session = SourceBookContentSession(
            source = source,
            entry = download.entry,
            media = media,
            externalResolver = AndroidBookExternalResourceResolver(application, networkHelper.client),
            materializationStore = materializationStore,
        )

        try {
            when (val opened = processor.open(session)) {
                is BookOpenResult.Failure -> return failure(
                    BookDownloadFailure.Reason.CONTENT_UNAVAILABLE,
                    opened.failure.message,
                )
                is BookOpenResult.Success -> opened.session.close()
            }
            val materialized = session.materializeResource(primaryResourceId).getOrElse { error ->
                return failure(BookDownloadFailure.Reason.NETWORK, error.message)
            }
            materialized.use { resource ->
                val staging = provider.beginPackage(source.name, download.entry, download.chapter).getOrElse { error ->
                    return failure(BookDownloadFailure.Reason.STORAGE, error.message)
                }
                try {
                    download.status = BookDownload.State.DOWNLOADING
                    val fileName = provider.resourceFileName(resource.metadata.id, resource.metadata.mediaType)
                    val output = staging.directory.createFile(fileName)
                        ?: throw IOException("Unable to create downloaded BOOK resource")
                    val copied = copyResource(resource.file, output, download)
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
                        catalogRevision = session.catalogRevision,
                        catalogCoverage = session.catalogCoverage,
                        resourceHierarchy = session.resourceHierarchy,
                        primaryResourceIds = listOf(primaryResourceId),
                        progressContentKey = progressIdentity.contentKey,
                        progressResourceId = progressIdentity.resourceKey,
                        progressResourceRevision = progressIdentity.resourceRevision,
                        resources = listOf(
                            BookDownloadedResource(
                                id = resource.metadata.id,
                                title = resource.metadata.title,
                                order = resource.metadata.order,
                                groupId = resource.metadata.groupId,
                                mediaType = resource.metadata.mediaType,
                                revision = resource.metadata.revision,
                                fileName = fileName,
                                storedSize = copied.size,
                                sha256 = copied.sha256,
                            ),
                        ),
                        createdAt = now(),
                    )
                    provider.completePackage(staging, manifest).getOrElse { error ->
                        throw BookPackageIntegrityException(error.message, error)
                    }
                } catch (error: CancellationException) {
                    staging.directory.delete()
                    throw error
                } catch (error: Exception) {
                    staging.directory.delete()
                    return failure(
                        if (error is BookPackageIntegrityException) {
                            BookDownloadFailure.Reason.INTEGRITY
                        } else {
                            BookDownloadFailure.Reason.STORAGE
                        },
                        error.message,
                    )
                }
            }
            cache.refresh()
            download.progress = 100
            download.status = BookDownload.State.DOWNLOADED
            return null
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
                    download.progress = ((copied * 95L) / expectedSize).toInt().coerceAtMost(95)
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

private class BookPackageIntegrityException(message: String?, cause: Throwable) : IOException(message, cause)

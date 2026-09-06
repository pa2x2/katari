package mihon.entry.interactions.book.format.epub.preparation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.normalizedBookContentLanguages
import mihon.entry.interactions.book.content.normalizedBookMediaType
import mihon.entry.interactions.book.document.preparation.BookDocumentCachedRemoteResource
import mihon.entry.interactions.book.document.preparation.BookDocumentCachedResource
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCache
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCacheKey
import mihon.entry.interactions.book.document.preparation.BookDocumentPreparedCacheValue
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.resource.BookPublicationResourceGatewayFactory
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.navigation.EpubNavigationParser
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackageParser
import mihon.entry.interactions.book.format.epub.resource.EpubArchiveResourceLoader
import mihon.entry.interactions.book.preparation.BookContentPreparer
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookRemoteResourceReference
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import java.io.File
import java.security.MessageDigest

internal class EpubBookPreparer(
    private val preparedCache: BookDocumentPreparedCache? = null,
    private val resourceGatewayFactory: BookPublicationResourceGatewayFactory? = null,
    private val documentPreparer: EpubDocumentPreparer = EpubDocumentPreparer(),
) : BookContentPreparer {
    override val id = "epub"
    override val outputModel = BookDocumentPublicationModel.DESCRIPTOR

    override fun supports(descriptor: mihon.book.api.BookContentDescriptor): Boolean =
        descriptor.format.substringBefore(';').trim().equals(EpubContract.FORMAT, true) &&
            descriptor.protection.equals("none", true)

    override suspend fun prepare(content: BookContentSession): BookPreparationResult {
        if (!supports(
                content.descriptor,
            )
        ) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "Unsupported EPUB descriptor")
        }
        val resourceId = content.primaryResourceIds.singleOrNull()
            ?: return failure(BookFailureReason.MALFORMED_CONTENT, "An EPUB must declare exactly one primary resource")
        var ownedArchive: EpubArchive? = null
        var ownedMaterialization: mihon.entry.interactions.book.content.MaterializedBookResource? = null
        return try {
            val resource = content.getResource(resourceId).getOrElse { error ->
                return failure(BookFailureReason.CONTENT_UNAVAILABLE, error.message ?: "The EPUB is unavailable", true)
            }
            require(
                resource.availability == BookResourceAvailability.UNKNOWN ||
                    resource.availability == BookResourceAvailability.AVAILABLE,
            ) { "The EPUB is not currently readable" }
            require(BookResourceCapability.MATERIALIZE in resource.capabilities) {
                "The EPUB cannot be materialized for reading"
            }
            require(resource.mediaType.normalizedBookMediaType()?.let { it == EpubContract.FORMAT } != false) {
                "The primary resource is not an EPUB"
            }
            val materialized = content.materializeResource(resourceId).getOrThrow()
            ownedMaterialization = materialized
            val contentDigest = withContext(Dispatchers.IO) { materialized.file.sha256() }
            val archive = withContext(Dispatchers.IO) { EpubArchive(materialized.file) }
            ownedArchive = archive
            val packageInfo = withContext(Dispatchers.Default) { EpubPackageParser(archive).parse() }
            val cacheKey = BookDocumentPreparedCacheKey(content.publicationId, contentDigest)
            val cached = withContext(Dispatchers.IO) { preparedCache?.read(cacheKey) }
            val derivedResources = cached?.derivedResources
                ?.associateTo(linkedMapOf()) { resource ->
                    resource.resourceId to BookPublicationResource(
                        resource.resourceId,
                        resource.mediaType,
                        resource.bytes,
                    )
                }
                ?: linkedMapOf()
            val remoteResources = cached?.remoteResources
                ?.associateTo(linkedMapOf()) { resource ->
                    resource.resourceId to BookRemoteResourceReference(
                        resource.resourceId,
                        resource.url,
                        BookRemoteResourceType.valueOf(resource.type),
                    )
                }
                ?: linkedMapOf()
            val preparedDocuments = cached?.model?.documents?.map { document ->
                EpubPreparedDocument(
                    document = document,
                    title = cached.documentTitles[document.resourceId],
                    hasReadableContent = document.blocks.any { it.content !is BookDocumentBlockContent.Unsupported },
                )
            } ?: packageInfo.documents.map { item ->
                if (!item.isRemote && documentPreparer.supports(item.mediaType)) {
                    documentPreparer.prepare(
                        archive,
                        packageInfo,
                        item,
                        contentDigest,
                        derivedResources,
                        remoteResources,
                    )
                } else {
                    EpubPreparedDocument(
                        document = documentPreparer.unsupported(item.resourceId, contentDigest),
                        title = null,
                        hasReadableContent = false,
                    )
                }
            }.also { generated ->
                withContext(Dispatchers.IO) {
                    preparedCache?.write(
                        cacheKey,
                        BookDocumentPreparedCacheValue(
                            model = BookDocumentPublicationModel(generated.map(EpubPreparedDocument::document)),
                            documentTitles = generated.associate { it.document.resourceId to it.title },
                            derivedResources = derivedResources.values.map { resource ->
                                BookDocumentCachedResource(
                                    resource.resourceId,
                                    requireNotNull(resource.mediaType),
                                    resource.bytes,
                                )
                            },
                            remoteResources = remoteResources.values.map { resource ->
                                BookDocumentCachedRemoteResource(
                                    resource.resourceId,
                                    resource.url,
                                    resource.type.name,
                                )
                            },
                        ),
                    )
                }
            }
            val documents = preparedDocuments.map(EpubPreparedDocument::document)
            val readingOrderIds = packageInfo.readingOrder.map(EpubManifestItem::resourceId).toSet()
            require(preparedDocuments.any { it.hasReadableContent && it.document.resourceId in readingOrderIds }) {
                "The EPUB contains no readable linear documents"
            }
            val titles = preparedDocuments.associate { it.document.resourceId to it.title }
            val publication = BookPublication(
                id = content.publicationId,
                revision = contentDigest,
                title = null,
                languages = (packageInfo.languages + content.languages).normalizedBookContentLanguages(),
                readingDirection = if (packageInfo.rightToLeft) {
                    BookReadingDirection.RIGHT_TO_LEFT
                } else {
                    BookReadingDirection.LEFT_TO_RIGHT
                },
                readingOrder = packageInfo.readingOrder.map { item ->
                    BookResource(item.resourceId, item.mediaType, titles[item.resourceId])
                },
                navigation = withContext(Dispatchers.Default) {
                    EpubNavigationParser(archive).parse(packageInfo)
                }.withDocumentTitles(titles),
            )
            val packagedResourceLoader = EpubArchiveResourceLoader(
                archive,
                packageInfo,
                derivedResources,
            )
            val resourceLoader = if (remoteResources.isNotEmpty()) {
                requireNotNull(resourceGatewayFactory) { "No remote publication resource gateway is available" }
                    .create(content.publicationId, contentDigest, packagedResourceLoader, remoteResources.values)
            } else {
                packagedResourceLoader
            }
            val prepared = PreparedBookDocumentPublication(
                publication = publication,
                model = BookDocumentPublicationModel(documents),
                resourceLoader = resourceLoader,
                locatorRevision = contentDigest,
                requiredResourceIds = emptySet(),
                resourceRequirements = emptyMap(),
                closeAction = {
                    archive.close()
                    materialized.close()
                },
            )
            ownedArchive = null
            ownedMaterialization = null
            BookPreparationResult.Success(prepared)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalArgumentException) {
            failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The EPUB is malformed")
        } catch (error: Exception) {
            failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The EPUB could not be prepared")
        } finally {
            ownedArchive?.close()
            ownedMaterialization?.close()
        }
    }

    private fun failure(
        reason: BookFailureReason,
        message: String,
        canRetry: Boolean = false,
    ) = BookPreparationResult.Failure(BookFailure(reason, message), canRetry)
}

private fun List<BookNavigationItem>.withDocumentTitles(titles: Map<String, String?>): List<BookNavigationItem> =
    map { item ->
        item.copy(
            title = item.title ?: titles[item.target.resourceId],
            children = item.children.withDocumentTitles(titles),
        )
    }

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}

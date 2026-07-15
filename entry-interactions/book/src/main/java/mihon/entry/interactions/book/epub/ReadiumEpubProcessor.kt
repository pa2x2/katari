package mihon.entry.interactions.book.epub

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.BookProcessor
import mihon.entry.interactions.book.BookPublicationSession
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookSessionCloseStack
import mihon.entry.interactions.book.MaterializedBookResource
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.DefaultArchiveOpener
import org.readium.r2.shared.util.asset.DefaultFormatSniffer
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.epub.EpubParser

internal class ReadiumEpubProcessor(
    private val archiveValidator: EpubArchiveValidator = EpubArchiveValidator(),
) : BookProcessor {
    override val id: String = "builtin.readium.epub"
    override val displayName: String = "EPUB reader"

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(
        resourceFactory = MaterializedFileOnlyResourceFactory,
        archiveOpener = DefaultArchiveOpener(),
        formatSniffer = DefaultFormatSniffer(),
    )
    private val publicationOpener = PublicationOpener(EpubParser(httpClient))

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == EPUB_MEDIA_TYPE &&
            descriptor.protection == "none" &&
            (descriptor.profile == null || descriptor.profile == REFLOWABLE_PROFILE)

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent {
        return ReadiumEpubReaderActivity.newIntent(context, request, id, sessionToken)
    }

    override suspend fun open(content: BookContentSession): BookOpenResult {
        if (!supports(content.descriptor)) {
            return BookOpenResult.Failure(
                BookFailure(BookFailureReason.FORMAT_UNSUPPORTED, "Unsupported EPUB descriptor"),
            )
        }

        val primaryResourceId = content.primaryResourceIds.singleOrNull()
            ?: return contentFailure("EPUB content must provide exactly one primary resource")
        val primaryResource = content.getResource(primaryResourceId).getOrElse {
            return contentFailure(it.message ?: "Unable to resolve the primary EPUB resource")
        }
        if (BookResourceCapability.MATERIALIZE !in primaryResource.capabilities) {
            return contentFailure("The primary EPUB resource cannot be materialized")
        }

        val lease = content.materializeResource(primaryResourceId).getOrElse {
            return BookOpenResult.Failure(
                BookFailure(BookFailureReason.CONTENT_UNAVAILABLE, it.message ?: "Unable to materialize EPUB"),
            )
        }
        if (lease.metadata.id != primaryResourceId) {
            lease.close()
            return contentFailure("Materialized EPUB resource identity does not match the requested resource")
        }
        archiveValidator.validate(lease.file)?.let { failure ->
            lease.invalidate()
            lease.close()
            return BookOpenResult.Failure(failure)
        }

        var asset: Asset? = null
        var publication: Publication? = null
        try {
            val assetResult = assetRetriever.retrieve(lease.file)
            asset = assetResult.getOrNull()
                ?: return failureAndClose(
                    lease,
                    BookFailureReason.MALFORMED_CONTENT,
                    assetResult.failureOrNull()?.message ?: "Unable to recognize EPUB",
                )
            val publicationResult = publicationOpener.open(asset, allowUserInteraction = false)
            publication = publicationResult.getOrNull()
                ?: run {
                    asset.close()
                    asset = null
                    return failureAndClose(
                        lease,
                        BookFailureReason.MALFORMED_CONTENT,
                        publicationResult.failureOrNull()?.message ?: "Unable to parse EPUB",
                    )
                }
            if (publication.metadata.layout != null && publication.metadata.layout != Layout.REFLOWABLE) {
                publication.close()
                publication = null
                return failureAndClose(
                    lease,
                    BookFailureReason.FORMAT_UNSUPPORTED,
                    "This EPUB uses a fixed or otherwise unsupported layout",
                )
            }
            if (publication.readingOrder.isEmpty()) {
                publication.close()
                publication = null
                return failureAndClose(
                    lease,
                    BookFailureReason.MALFORMED_CONTENT,
                    "EPUB publication has no readable content",
                )
            }
            return BookOpenResult.Success(
                ReadiumPublicationSession(
                    enginePublication = publication,
                    lease = lease,
                    publicationId = content.publicationId,
                    revision = content.revision,
                ),
            ).also {
                publication = null
                asset = null
            }
        } catch (error: CancellationException) {
            publication?.close() ?: asset?.close()
            lease.close()
            throw error
        } catch (error: Exception) {
            publication?.close() ?: asset?.close()
            return failureAndClose(
                lease,
                BookFailureReason.MALFORMED_CONTENT,
                error.message ?: "Unexpected EPUB error",
            )
        }
    }

    private fun failureAndClose(
        lease: MaterializedBookResource,
        reason: BookFailureReason,
        message: String,
    ): BookOpenResult.Failure {
        if (reason == BookFailureReason.MALFORMED_CONTENT) lease.invalidate()
        lease.close()
        return BookOpenResult.Failure(BookFailure(reason, message))
    }

    private fun contentFailure(message: String): BookOpenResult.Failure =
        BookOpenResult.Failure(BookFailure(BookFailureReason.CONTENT_UNAVAILABLE, message))

    private companion object {
        const val EPUB_MEDIA_TYPE = "application/epub+zip"
        const val REFLOWABLE_PROFILE = "reflowable"
    }
}

internal class ReadiumPublicationSession(
    private val enginePublication: Publication,
    private val lease: MaterializedBookResource,
    publicationId: String,
    revision: String,
) : BookPublicationSession {
    private val closeStack = BookSessionCloseStack().apply {
        own(lease)
        own(AutoCloseable(enginePublication::close))
    }

    override val publication: BookPublication = ReadiumPublicationAdapter.adapt(
        publication = enginePublication,
        publicationId = publicationId,
        revision = revision,
    )

    override fun validate(locator: BookLocator): Boolean =
        ReadiumLocatorAdapter.restore(locator, enginePublication) != null

    fun readiumPublication(): Publication = enginePublication

    override fun close() = closeStack.close()
}

private object MaterializedFileOnlyResourceFactory : ResourceFactory {
    override suspend fun create(url: AbsoluteUrl): Try<Resource, ResourceFactory.Error> =
        Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))
}

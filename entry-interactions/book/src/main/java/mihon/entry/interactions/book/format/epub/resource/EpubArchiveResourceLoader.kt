package mihon.entry.interactions.book.format.epub.resource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackage
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import java.security.MessageDigest

/** Renderer access to publication resources packaged inside the primary container. */
internal class EpubArchiveResourceLoader(
    private val archive: EpubArchive,
    packageInfo: EpubPackage,
    private val derivedResources: Map<String, BookPublicationResource> = emptyMap(),
) : BookPublicationResourceLoader {
    private val itemsByResource = packageInfo.manifest.values.associateBy(EpubManifestItem::resourceId)
    private val uniqueIdentifier = packageInfo.uniqueIdentifier
    private val protectionAlgorithms = packageInfo.resourceProtectionAlgorithms

    override suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource> = try {
        derivedResources[resourceId]?.let { resource ->
            require(resource.mediaType in acceptedMediaTypes) {
                "Publication resource has unsupported media type ${resource.mediaType}"
            }
            require(resource.bytes.size in 1..maxBytes) { "Publication resource exceeds its byte limit" }
            return Result.success(resource)
        }
        val item = requireNotNull(itemsByResource[resourceId]) {
            "Publication resource is not declared in its manifest"
        }
        require(item.mediaType in acceptedMediaTypes) {
            "Publication resource has unsupported media type ${item.mediaType}"
        }
        val bytes = withContext(Dispatchers.IO) { archive.read(resourceId, maxBytes) }
            .deobfuscated(resourceId)
        require(bytes.isNotEmpty()) { "Publication resource is empty" }
        Result.success(BookPublicationResource(resourceId, item.mediaType, bytes))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun ByteArray.deobfuscated(resourceId: String): ByteArray = when (protectionAlgorithms[resourceId]) {
        null -> this
        IDPF_ALGORITHM -> xorPrefix(
            MessageDigest.getInstance("SHA-1").digest(
                requireNotNull(uniqueIdentifier) { "Obfuscated font has no publication identifier" }
                    .filterNot(Char::isWhitespace)
                    .encodeToByteArray(),
            ),
            1_040,
        )
        ADOBE_ALGORITHM -> xorPrefix(
            requireNotNull(uniqueIdentifier) { "Obfuscated font has no publication identifier" }
                .removePrefix("urn:uuid:")
                .replace("-", "")
                .chunked(2)
                .takeIf { it.size == 16 }
                ?.map { value -> value.toInt(16).toByte() }
                ?.toByteArray()
                ?: error("Obfuscated font has an invalid publication identifier"),
            1_024,
        )
        else -> error("Publication resource uses unsupported encryption")
    }

    private fun ByteArray.xorPrefix(key: ByteArray, limit: Int): ByteArray = copyOf().also { output ->
        repeat(minOf(output.size, limit)) { index ->
            output[index] = (output[index].toInt() xor key[index % key.size].toInt()).toByte()
        }
    }

    private companion object {
        const val IDPF_ALGORITHM = "http://www.idpf.org/2008/embedding"
        const val ADOBE_ALGORITHM = "http://ns.adobe.com/pdf/enc#RC"
    }
}

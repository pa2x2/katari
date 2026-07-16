package mihon.entry.interactions.book.download

import kotlinx.serialization.Serializable
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResourceGroup

/** Stable identity of one downloaded, openable BOOK child. */
internal data class BookDownloadPackageKey(
    val sourceId: Long,
    val entryUrl: String,
    val childUrl: String,
) {
    init {
        require(entryUrl.isNotBlank()) { "entry URL must not be blank" }
        require(childUrl.isNotBlank()) { "child URL must not be blank" }
    }
}

/** Durable, versioned description of a complete offline BOOK package. */
@Serializable
internal data class BookDownloadManifest(
    val version: Int = CURRENT_VERSION,
    val sourceId: Long,
    val entryId: Long,
    val entryTitle: String,
    val entryUrl: String,
    val childId: Long,
    val childTitle: String,
    val childUrl: String,
    val descriptor: BookContentDescriptor,
    val publicationId: String,
    val publicationRevision: String,
    val catalogRevision: String? = null,
    val catalogCoverage: BookCatalogCoverage = BookCatalogCoverage.UNKNOWN,
    val resourceHierarchy: List<BookContentResourceGroup> = emptyList(),
    val primaryResourceIds: List<String>,
    val resources: List<BookDownloadedResource>,
    val createdAt: Long,
) {
    val packageKey: BookDownloadPackageKey
        get() = BookDownloadPackageKey(sourceId, entryUrl, childUrl)

    init {
        require(version == CURRENT_VERSION) { "unsupported BOOK download manifest version: $version" }
        require(entryTitle.isNotBlank()) { "entry title must not be blank" }
        require(entryUrl.isNotBlank()) { "entry URL must not be blank" }
        require(childTitle.isNotBlank()) { "child title must not be blank" }
        require(childUrl.isNotBlank()) { "child URL must not be blank" }
        require(publicationId.isNotBlank()) { "publication id must not be blank" }
        require(publicationRevision.isNotBlank()) { "publication revision must not be blank" }
        require(catalogRevision == null || catalogRevision.isNotBlank()) { "catalog revision must not be blank" }
        require(primaryResourceIds.isNotEmpty()) { "a BOOK download must have a primary resource" }
        require(primaryResourceIds.none(String::isBlank)) { "primary resource ids must not be blank" }
        require(primaryResourceIds.distinct().size == primaryResourceIds.size) {
            "primary resource ids must be unique"
        }
        require(resources.isNotEmpty()) { "a BOOK download must contain resources" }
        require(resources.distinctBy(BookDownloadedResource::id).size == resources.size) {
            "downloaded resource ids must be unique"
        }
        require(resources.distinctBy(BookDownloadedResource::fileName).size == resources.size) {
            "downloaded resource filenames must be unique"
        }
        require(primaryResourceIds.all(resources.map(BookDownloadedResource::id).toSet()::contains)) {
            "primary resources must be present in the downloaded package"
        }
        require(createdAt >= 0L) { "creation time must not be negative" }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Integrity and processor metadata for one file in an offline BOOK package. */
@Serializable
internal data class BookDownloadedResource(
    val id: String,
    val title: String? = null,
    val order: Long? = null,
    val groupId: String? = null,
    val mediaType: String? = null,
    val revision: String? = null,
    val fileName: String,
    val storedSize: Long,
    val sha256: String,
) {
    init {
        require(id.isNotBlank()) { "resource id must not be blank" }
        require(order == null || order >= 0L) { "resource order must not be negative" }
        require(groupId == null || groupId.isNotBlank()) { "resource group id must not be blank" }
        require(mediaType == null || mediaType.isNotBlank()) { "resource media type must not be blank" }
        require(revision == null || revision.isNotBlank()) { "resource revision must not be blank" }
        require(fileName.isSafePackageFileName()) { "resource filename is not package-local" }
        require(fileName != BookDownloadProvider.MANIFEST_FILE_NAME) { "resource filename conflicts with the manifest" }
        require(storedSize > 0L) { "stored resource size must be positive" }
        require(SHA256_PATTERN.matches(sha256)) { "resource SHA-256 must be lowercase hexadecimal" }
    }
}

private fun String.isSafePackageFileName(): Boolean =
    isNotBlank() && this != "." && this != ".." && '/' !in this && '\\' !in this

private val SHA256_PATTERN = Regex("[a-f0-9]{64}")

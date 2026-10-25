package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookResourceAvailability

/**
 * A bounded snapshot of source-known resources for one book publication.
 *
 * @property resources resources known at the catalog [revision].
 * @property revision optional source-defined revision for the catalog itself.
 * @property coverage whether this snapshot is complete, partial, or expected to grow.
 */
@Serializable
data class BookResourceCatalog(
    val resources: List<BookSourceResource> = emptyList(),
    val revision: String? = null,
    val coverage: BookCatalogCoverage = BookCatalogCoverage.UNKNOWN,
) {
    init {
        require(revision == null || revision.isNotBlank()) { "catalog revision must not be blank" }
        require(resources.size <= MAX_RESOURCES) { "catalog must not exceed $MAX_RESOURCES resources" }
        require(resources.distinctBy(BookSourceResource::id).size == resources.size) {
            "catalog resource ids must be unique"
        }
    }

    /** Source catalog safety limits. */
    companion object {
        /** Maximum number of resources carried by one source media result. */
        const val MAX_RESOURCES = 10_000
    }
}

/**
 * Source metadata and an optional data-only retrieval location for a publication resource.
 *
 * @property id stable publication-scoped identity used by reader progress.
 * @property title optional user-visible title.
 * @property order optional zero-based source ordering hint.
 * @property groupId optional stable grouping hint.
 * @property mediaType optional resource media type.
 * @property size optional byte size.
 * @property revision optional revision for this resource, independent of the catalog revision.
 * @property availability current source-side availability.
 * @property location optional location from which Katari can resolve the resource.
 */
@Serializable
data class BookSourceResource(
    val id: String,
    val title: String? = null,
    val order: Long? = null,
    val groupId: String? = null,
    val mediaType: String? = null,
    val size: Long? = null,
    val revision: String? = null,
    val availability: BookResourceAvailability = BookResourceAvailability.UNKNOWN,
    val location: BookResourceLocation? = null,
) {
    init {
        require(id.isNotBlank()) { "resource id must not be blank" }
        require(order == null || order >= 0L) { "resource order must not be negative" }
        require(groupId == null || groupId.isNotBlank()) { "resource group id must not be blank" }
        require(mediaType == null || mediaType.isNotBlank()) { "resource media type must not be blank" }
        require(size == null || size >= 0L) { "resource size must not be negative" }
        require(revision == null || revision.isNotBlank()) { "resource revision must not be blank" }
    }
}

/**
 * Optional source-provided grouping hierarchy for entry-screen resource projection.
 *
 * @property id stable hierarchy-node identity.
 * @property title optional user-visible group title.
 * @property resourceIds publication resource IDs directly contained by this node.
 * @property children nested groups.
 */
@Serializable
data class BookResourceHierarchyNode(
    val id: String,
    val title: String? = null,
    val resourceIds: List<String> = emptyList(),
    val children: List<BookResourceHierarchyNode> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "hierarchy id must not be blank" }
        require(resourceIds.none(String::isBlank)) { "hierarchy resource ids must not be blank" }
    }
}

/**
 * How Katari fetches resources that a publication's documents name by URL instead of by catalog ID.
 *
 * Prose chapters may write an `<img>` source as an absolute http(s) URL, a URL relative to [baseUrl],
 * or a `data:` image. A source that matches a catalog resource ID still names that resource. Katari fetches
 * the rest itself, reads their media type from their bytes and keeps them in downloads. An image that is gone
 * shows its alternative text, and its chapter still opens and downloads.
 *
 * @property baseUrl absolute http(s) URL that relative sources resolve against. When null, the primary
 *   resource's [BookResourceLocation.RemoteRequest] URL, if it has one.
 * @property headers sent with every request for a linked resource, whichever host it is on. A redirect to
 *   another origin keeps only Accept, Accept-Language and User-Agent.
 */
@Serializable
data class BookLinkedResources(
    val baseUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    init {
        require(baseUrl == null || baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
            "linked resource base URL must use HTTP or HTTPS"
        }
        require(headers.keys.none(String::isBlank)) { "header names must not be blank" }
    }
}

/** A closed, serializable description of where Katari can resolve a book resource. */
@Serializable
sealed interface BookResourceLocation {
    /**
     * A resource fetched through the existing [UnifiedSource.getMedia] child API.
     *
     * @property resourceId stable publication resource identity used by progress.
     * @property sourceChildKey source-specific child identity used for retrieval.
     */
    @Serializable
    @SerialName("source_child")
    data class SourceChild(
        val resourceId: String,
        val sourceChildKey: String,
    ) : BookResourceLocation {
        init {
            require(resourceId.isNotBlank()) { "resource id must not be blank" }
            require(sourceChildKey.isNotBlank()) { "source child key must not be blank" }
        }
    }

    /**
     * An HTTP(S) request resolved by Katari without exposing credentials to a processor.
     *
     * @property url absolute HTTP(S) resource URL.
     * @property headers request headers required by the source host.
     */
    @Serializable
    @SerialName("remote_request")
    data class RemoteRequest(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
    ) : BookResourceLocation {
        init {
            require(url.isNotBlank()) { "remote URL must not be blank" }
            require(url.startsWith("https://") || url.startsWith("http://")) {
                "remote URL must use HTTP or HTTPS"
            }
            require(headers.keys.none(String::isBlank)) { "header names must not be blank" }
        }
    }

    /**
     * Textual content held in memory, such as a chapter the source assembled itself.
     * The content's format may set a lower limit; prose chapters accept at most 4 MiB of UTF-8.
     *
     * @property text inline resource text.
     * @property mediaType optional authoritative media type.
     */
    @Serializable
    @SerialName("inline_text")
    data class InlineText(
        val text: String,
        val mediaType: String? = null,
    ) : BookResourceLocation {
        init {
            require(text.length <= MAX_INLINE_TEXT_LENGTH) {
                "inline text must not exceed $MAX_INLINE_TEXT_LENGTH characters"
            }
            require(mediaType == null || mediaType.isNotBlank()) { "inline text media type must not be blank" }
        }
    }

    /**
     * Small inline binary content. Large content must use another location type.
     *
     * @property bytes inline resource bytes.
     * @property mediaType optional authoritative media type.
     */
    @Serializable
    @SerialName("inline_bytes")
    class InlineBytes(
        val bytes: ByteArray,
        val mediaType: String? = null,
    ) : BookResourceLocation {
        init {
            require(bytes.size <= MAX_INLINE_BYTES) { "inline bytes must not exceed $MAX_INLINE_BYTES bytes" }
            require(mediaType == null || mediaType.isNotBlank()) { "inline bytes media type must not be blank" }
        }

        /** Compares inline byte content and media type. */
        override fun equals(other: Any?): Boolean {
            return other is InlineBytes && bytes.contentEquals(other.bytes) && mediaType == other.mediaType
        }

        /** Hashes inline byte content and media type. */
        override fun hashCode(): Int = 31 * bytes.contentHashCode() + mediaType.hashCode()

        /** Returns metadata without printing inline byte content. */
        override fun toString(): String = "InlineBytes(bytes=<${bytes.size} bytes>, mediaType=$mediaType)"
    }

    /**
     * A content-provider URI whose access remains owned by Katari.
     *
     * @property uri absolute `content://` URI.
     */
    @Serializable
    @SerialName("local_uri")
    data class LocalUri(val uri: String) : BookResourceLocation {
        init {
            require(uri.startsWith("content://")) { "local URI must use a content scheme" }
        }
    }

    /**
     * An opaque reference to a resource already owned by Katari.
     *
     * @property id app-issued reference identity.
     */
    @Serializable
    @SerialName("app_reference")
    data class AppReference(val id: String) : BookResourceLocation {
        init {
            require(id.isNotBlank()) { "app reference id must not be blank" }
        }
    }

    /** Inline resource safety limits. */
    companion object {
        /** Maximum UTF-16 character count for inline text. */
        const val MAX_INLINE_TEXT_LENGTH = 4 * 1024 * 1024

        /** Maximum byte count for inline binary content. */
        const val MAX_INLINE_BYTES = 256 * 1024
    }
}

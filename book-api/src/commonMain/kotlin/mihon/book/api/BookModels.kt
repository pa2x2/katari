package mihon.book.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Processor-selection metadata for source-provided book content. */
@Serializable
data class BookContentDescriptor(
    val format: String,
    val profile: String? = null,
    val protection: String = "none",
) {
    init {
        require(format.isNotBlank()) { "format must not be blank" }
        require(profile == null || profile.isNotBlank()) { "profile must not be blank" }
        require(protection.isNotBlank()) { "protection must not be blank" }
    }
}

/** Processor-facing metadata and access capabilities for one publication resource. */
@Serializable
data class BookContentResource(
    val id: String,
    val mediaType: String? = null,
    val size: Long? = null,
    val revision: String? = null,
    val availability: BookResourceAvailability = BookResourceAvailability.UNKNOWN,
    val cacheState: BookResourceCacheState = BookResourceCacheState.UNKNOWN,
    val capabilities: Set<BookResourceCapability> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "resource id must not be blank" }
        require(mediaType == null || mediaType.isNotBlank()) { "resource media type must not be blank" }
        require(size == null || size >= 0) { "resource size must not be negative" }
        require(revision == null || revision.isNotBlank()) { "resource revision must not be blank" }
    }
}

/** One cursor-paged segment of processor-facing resources. */
@Serializable
data class BookContentResourcePage(
    val resources: List<BookContentResource>,
    val nextCursor: String? = null,
) {
    init {
        require(nextCursor == null || nextCursor.isNotBlank()) { "next cursor must not be blank" }
        require(resources.distinctBy(BookContentResource::id).size == resources.size) {
            "resource page ids must be unique"
        }
    }
}

/** Access modes that Katari can provide for a resource. */
@Serializable
enum class BookResourceCapability {
    STREAM,
    RANGE,
    MATERIALIZE,
}

/** Current state of a resource in Katari-owned durable storage. */
@Serializable
enum class BookResourceCacheState {
    UNKNOWN,
    NOT_CACHED,
    PARTIALLY_CACHED,
    CACHED,
}

/** Structured source-side reason that a resource may not currently be readable. */
@Serializable
enum class BookResourceAvailability {
    UNKNOWN,
    AVAILABLE,
    AUTHENTICATION_REQUIRED,
    PURCHASE_REQUIRED,
    UNSUPPORTED_APP_ACCESS,
    REMOVED,
    REGION_RESTRICTED,
}

/** Completeness and growth semantics of a source resource catalog. */
@Serializable
enum class BookCatalogCoverage {
    UNKNOWN,
    COMPLETE,
    PARTIAL,
    ONGOING,
}

/** Processor-normalized publication metadata used by Katari and processor-owned readers. */
@Serializable
data class BookPublication(
    val id: String,
    val revision: String,
    val title: String?,
    val languages: List<String>,
    val readingDirection: BookReadingDirection?,
    val readingOrder: List<BookResource>,
    val navigation: List<BookNavigationItem>,
) {
    init {
        require(id.isNotBlank()) { "publication id must not be blank" }
        require(revision.isNotBlank()) { "publication revision must not be blank" }
        require(languages.none(String::isBlank)) { "publication languages must not be blank" }
        require(readingOrder.distinctBy(BookResource::id).size == readingOrder.size) {
            "reading order resource ids must be unique"
        }
    }
}

/** A normalized readable publication resource. */
@Serializable
data class BookResource(
    val id: String,
    val mediaType: String?,
    val title: String?,
) {
    init {
        require(id.isNotBlank()) { "resource id must not be blank" }
        require(mediaType == null || mediaType.isNotBlank()) { "resource media type must not be blank" }
    }
}

/** A recursive publication navigation item targeting a [BookLocator]. */
@Serializable
data class BookNavigationItem(
    val title: String?,
    val target: BookLocator,
    val children: List<BookNavigationItem> = emptyList(),
)

/** Logical reading direction of a publication. */
@Serializable
enum class BookReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

/** Processor-independent persisted reading location with namespaced precision extensions. */
@Serializable
data class BookLocator(
    val resourceId: String,
    val progression: Double? = null,
    val totalProgression: Double? = null,
    val logicalPosition: Int? = null,
    val fragments: List<String> = emptyList(),
    val textContext: BookTextContext? = null,
    val extensions: Map<String, JsonElement> = emptyMap(),
) {
    init {
        require(resourceId.isNotBlank()) { "resourceId must not be blank" }
        require(progression == null || (progression.isFinite() && progression in 0.0..1.0)) {
            "progression must be finite and between 0 and 1"
        }
        require(totalProgression == null || (totalProgression.isFinite() && totalProgression in 0.0..1.0)) {
            "totalProgression must be finite and between 0 and 1"
        }
        require(logicalPosition == null || logicalPosition >= 1) { "logicalPosition must be at least 1" }
        require(fragments.none(String::isBlank)) { "locator fragments must not be blank" }
        require(extensions.keys.none(String::isBlank)) { "locator extension names must not be blank" }
    }
}

/** Bounded text surrounding a locator, used for revision reconciliation. */
@Serializable
data class BookTextContext(
    val before: String? = null,
    val highlight: String? = null,
    val after: String? = null,
) {
    init {
        require(listOfNotNull(before, highlight, after).all { it.length <= MAX_LENGTH }) {
            "text context fields must not exceed $MAX_LENGTH characters"
        }
    }

    companion object {
        const val MAX_LENGTH = 256
    }
}

/** Structured processor/content failure suitable for the generic BOOK host. */
@Serializable
data class BookFailure(
    val reason: BookFailureReason,
    val message: String,
) {
    init {
        require(message.isNotBlank()) { "failure message must not be blank" }
    }
}

/** Stable categories of BOOK processor and content failures. */
@Serializable
enum class BookFailureReason {
    CONTENT_UNAVAILABLE,
    FORMAT_UNSUPPORTED,
    MALFORMED_CONTENT,
    PROCESSOR_UNAVAILABLE,
    CANCELLED,
}

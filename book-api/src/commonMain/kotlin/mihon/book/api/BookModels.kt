package mihon.book.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Processor-selection metadata for source-provided book content.
 *
 * @property format open identifier for the publication format.
 * @property profile optional format profile used to refine processor selection.
 * @property protection protection scheme identifier, or `none` for unprotected content.
 */
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

/**
 * Processor-facing metadata and access capabilities for one publication resource.
 *
 * @property id stable publication-scoped resource identity.
 * @property title optional user-visible resource title.
 * @property order optional zero-based ordering hint.
 * @property groupId optional stable resource-group identity.
 * @property mediaType optional resource media type.
 * @property size optional resource size in bytes.
 * @property revision optional resource revision independent of its containing catalog.
 * @property availability current source-side availability.
 * @property cacheState current state in Katari-owned durable storage.
 * @property capabilities access modes Katari can provide to a processor.
 */
@Serializable
data class BookContentResource(
    val id: String,
    val title: String? = null,
    val order: Long? = null,
    val groupId: String? = null,
    val mediaType: String? = null,
    val size: Long? = null,
    val revision: String? = null,
    val availability: BookResourceAvailability = BookResourceAvailability.UNKNOWN,
    val cacheState: BookResourceCacheState = BookResourceCacheState.UNKNOWN,
    val capabilities: Set<BookResourceCapability> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "resource id must not be blank" }
        require(order == null || order >= 0L) { "resource order must not be negative" }
        require(groupId == null || groupId.isNotBlank()) { "resource group id must not be blank" }
        require(mediaType == null || mediaType.isNotBlank()) { "resource media type must not be blank" }
        require(size == null || size >= 0) { "resource size must not be negative" }
        require(revision == null || revision.isNotBlank()) { "resource revision must not be blank" }
    }
}

/**
 * Processor-facing nested grouping hints for catalog resources.
 *
 * @property id stable group identity.
 * @property title optional user-visible group title.
 * @property resourceIds resource identities directly contained by this group.
 * @property children nested resource groups.
 */
@Serializable
data class BookContentResourceGroup(
    val id: String,
    val title: String? = null,
    val resourceIds: List<String> = emptyList(),
    val children: List<BookContentResourceGroup> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "resource group id must not be blank" }
        require(resourceIds.none(String::isBlank)) { "group resource ids must not be blank" }
        require(resourceIds.distinct().size == resourceIds.size) { "group resource ids must be unique" }
    }
}

/**
 * One cursor-paged segment of processor-facing resources.
 *
 * @property resources resources in this page.
 * @property nextCursor opaque cursor for the next page, or `null` when complete.
 */
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

/**
 * Processor-normalized publication metadata used by Katari and processor-owned readers.
 *
 * @property id stable normalized publication identity.
 * @property revision normalized publication revision.
 * @property title optional publication title.
 * @property languages publication language tags.
 * @property readingDirection optional logical reading direction.
 * @property readingOrder ordered readable resources.
 * @property navigation recursive publication navigation tree.
 */
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

/**
 * A normalized readable publication resource.
 *
 * @property id stable publication-scoped resource identity.
 * @property mediaType optional normalized resource media type.
 * @property title optional user-visible resource title.
 */
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

/**
 * A recursive publication navigation item targeting a [BookLocator].
 *
 * @property title optional user-visible navigation label.
 * @property target location opened by this item.
 * @property children nested navigation items.
 */
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

/**
 * Processor-independent persisted reading location with namespaced precision extensions.
 *
 * @property resourceId publication resource containing this location.
 * @property progression normalized progression within the resource.
 * @property totalProgression normalized progression through the publication.
 * @property logicalPosition optional one-based logical position such as a page number.
 * @property fragments processor-defined stable location fragments.
 * @property textContext bounded surrounding text used for revision reconciliation.
 * @property extensions namespaced processor-specific precision data.
 */
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

/**
 * Bounded text surrounding a locator, used for revision reconciliation.
 *
 * @property before text immediately before the selected location.
 * @property highlight text at the selected location.
 * @property after text immediately after the selected location.
 */
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

    /** Text-context safety limits. */
    companion object {
        /** Maximum length of each text-context field. */
        const val MAX_LENGTH = 256
    }
}

/**
 * Structured processor/content failure suitable for the generic BOOK host.
 *
 * @property reason stable failure category.
 * @property message actionable user-facing failure detail.
 */
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

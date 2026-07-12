package mihon.book.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BookContentDescriptor(
    val format: String,
    val profile: String? = null,
    val protection: String = "none",
)

@Serializable
data class BookPublication(
    val id: String,
    val revision: String,
    val title: String?,
    val languages: List<String>,
    val readingDirection: BookReadingDirection?,
    val readingOrder: List<BookResource>,
    val navigation: List<BookNavigationItem>,
)

@Serializable
data class BookResource(
    val id: String,
    val mediaType: String?,
    val title: String?,
)

@Serializable
data class BookNavigationItem(
    val title: String?,
    val target: BookLocator,
    val children: List<BookNavigationItem> = emptyList(),
)

@Serializable
enum class BookReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

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
    }
}

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

@Serializable
data class BookFailure(
    val reason: BookFailureReason,
    val message: String,
)

@Serializable
enum class BookFailureReason {
    CONTENT_UNAVAILABLE,
    FORMAT_UNSUPPORTED,
    MALFORMED_CONTENT,
    PROCESSOR_UNAVAILABLE,
    CANCELLED,
}

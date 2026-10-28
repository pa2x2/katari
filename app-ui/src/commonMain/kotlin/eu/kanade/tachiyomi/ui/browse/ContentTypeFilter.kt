package eu.kanade.tachiyomi.ui.browse

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType

private const val UNSPECIFIED_ENTRY_TYPE = "__UNSPECIFIED__"

@Immutable
data class ContentTypeFilter(
    val entryTypes: Set<EntryType> = emptySet(),
    val includeUnspecified: Boolean = false,
) {
    val isActive: Boolean
        get() = entryTypes.isNotEmpty() || includeUnspecified

    fun matches(supportedEntryTypes: Set<EntryType>?): Boolean {
        if (!isActive) return true

        return if (supportedEntryTypes.isNullOrEmpty()) {
            includeUnspecified
        } else {
            supportedEntryTypes.any(entryTypes::contains)
        }
    }

    fun toPreferenceValue(): Set<String> {
        return buildSet {
            entryTypes.mapTo(this) { it.name }
            if (includeUnspecified) add(UNSPECIFIED_ENTRY_TYPE)
        }
    }

    companion object {
        fun fromPreferenceValue(value: Set<String>): ContentTypeFilter {
            return ContentTypeFilter(
                entryTypes = value.mapNotNullTo(linkedSetOf()) { storedValue ->
                    EntryType.entries.find { it.name == storedValue }
                },
                includeUnspecified = UNSPECIFIED_ENTRY_TYPE in value,
            )
        }
    }
}

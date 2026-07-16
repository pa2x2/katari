package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.runtime.Immutable
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter

typealias ItemGroups = Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

object ExtensionUiModel {
    sealed interface Header {
        data class Resource(val textRes: StringResource) : Header
        data class Text(val text: String) : Header
    }

    data class Item(
        val extension: Extension,
        val installStep: InstallStep,
    )
}

@Immutable
data class ExtensionListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: ItemGroups = mutableMapOf(),
    val updates: Int = 0,
    val installer: BasePreferences.ExtensionInstaller? = null,
    val searchQuery: String? = null,
    val filter: ExtensionFilterState = ExtensionFilterState(),
) {
    val isEmpty = items.isEmpty()

    val visibleLanguageCount: Int
        get() = items.values
            .asSequence()
            .flatten()
            .flatMap { it.extension.languagesForDisplay().asSequence() }
            .distinct()
            .count()
}

@Immutable
data class ExtensionFilterState(
    val languages: List<String> = emptyList(),
    val enabledLanguages: Set<String> = emptySet(),
    val contentTypes: ContentTypeFilter = ContentTypeFilter(),
) {
    val enabledLanguageCount: Int
        get() = languages.count(enabledLanguages::contains)

    val hasLanguageFilter: Boolean
        get() = languages.isNotEmpty() && enabledLanguageCount < languages.size

    val activeFilterCount: Int
        get() = (if (contentTypes.isActive) 1 else 0) + (if (hasLanguageFilter) 1 else 0)

    val selectedEntryTypes: Set<EntryType>
        get() = contentTypes.entryTypes
}

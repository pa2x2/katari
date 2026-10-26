package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class ExtensionDetailsSourceUiModel(
    val id: Long,
    val name: String,
    val title: String,
    val lang: String,
    val labelAsName: Boolean,
    val enabled: Boolean,
    val hasSettings: Boolean,
    val supportedEntryTypes: Set<EntryType>?,
)

sealed interface ExtensionDetailsState {

    @Immutable
    data object Loading : ExtensionDetailsState

    @Immutable
    data object Uninstalled : ExtensionDetailsState

    @Immutable
    data class Success(
        val extension: Extension.Loaded,
        val isIncognito: Boolean,
        val sources: ImmutableList<ExtensionDetailsSourceUiModel>,
        /** A file extension's site, which Extension info offers where an apk has App info; `null` for an apk. */
        val website: String? = null,
    ) : ExtensionDetailsState
}

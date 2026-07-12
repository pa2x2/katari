package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ExtensionDetailsSourceUiModel(
    val id: Long,
    val name: String,
    val title: String,
    val lang: String,
    val labelAsName: Boolean,
    val enabled: Boolean,
    val hasSettings: Boolean,
)

@Immutable
data class ExtensionDetailsState(
    val extension: Extension.Installed? = null,
    val isIncognito: Boolean = false,
    private val _sources: ImmutableList<ExtensionDetailsSourceUiModel>? = null,
) {
    val sources: ImmutableList<ExtensionDetailsSourceUiModel>
        get() = _sources ?: persistentListOf()

    val isLoading: Boolean
        get() = extension == null || _sources == null

    fun copyWithSources(sources: ImmutableList<ExtensionDetailsSourceUiModel>): ExtensionDetailsState {
        return copy(_sources = sources)
    }
}

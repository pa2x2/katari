package eu.kanade.domain.source.interactor

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.browse.SourceUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SourceListState(
    val isLoading: Boolean = true,
    val items: ImmutableList<SourceUiModel> = persistentListOf(),
    /** Whether any source was listed before the name query; false means there is nothing to find. */
    val hasSources: Boolean = false,
) {
    val isEmpty = items.isEmpty()

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}

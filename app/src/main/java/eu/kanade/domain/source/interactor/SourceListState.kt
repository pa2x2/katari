package eu.kanade.domain.source.interactor

import androidx.compose.runtime.Immutable
import eu.kanade.presentation.browse.SourceUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SourceListState(
    val isLoading: Boolean = true,
    val items: ImmutableList<SourceUiModel> = persistentListOf(),
) {
    val isEmpty = items.isEmpty()

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}

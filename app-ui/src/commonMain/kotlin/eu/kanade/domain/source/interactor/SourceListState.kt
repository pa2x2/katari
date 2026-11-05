package eu.kanade.domain.source.interactor

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.source.model.Source

@Immutable
data class SourceListState(
    val isLoading: Boolean = true,
    val groups: ImmutableList<SourceGroup> = persistentListOf(),
    /** Whether any source was listed before the name query; false means there is nothing to find. */
    val hasSources: Boolean = false,
) {
    val isEmpty = groups.isEmpty()

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}

/** The sources listed under one heading: a language, or [SourceListState.PINNED_KEY] or [SourceListState.LAST_USED_KEY]. */
@Immutable
data class SourceGroup(val language: String, val sources: ImmutableList<Source>)

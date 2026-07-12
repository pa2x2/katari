package eu.kanade.domain.source.interactor

import eu.kanade.presentation.browse.SourceUiModel
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.source.model.Pin
import tachiyomi.domain.source.model.Source
import java.util.TreeMap

object SourceListUiMapper {

    fun map(sources: List<Source>): SourceListState {
        val map = TreeMap<String, MutableList<Source>> { d1, d2 ->
            when {
                d1 == SourceListState.LAST_USED_KEY && d2 != SourceListState.LAST_USED_KEY -> -1
                d2 == SourceListState.LAST_USED_KEY && d1 != SourceListState.LAST_USED_KEY -> 1
                d1 == SourceListState.PINNED_KEY && d2 != SourceListState.PINNED_KEY -> -1
                d2 == SourceListState.PINNED_KEY && d1 != SourceListState.PINNED_KEY -> 1
                d1 == "" && d2 != "" -> 1
                d2 == "" && d1 != "" -> -1
                else -> d1.compareTo(d2)
            }
        }

        val byLang = sources.groupByTo(map) {
            when {
                it.isUsedLast -> SourceListState.LAST_USED_KEY
                Pin.Actual in it.pin -> SourceListState.PINNED_KEY
                else -> it.lang
            }
        }

        return SourceListState(
            isLoading = false,
            items = byLang
                .flatMap {
                    listOf(
                        SourceUiModel.Header(it.key),
                        *it.value.map { source ->
                            SourceUiModel.Item(source)
                        }.toTypedArray(),
                    )
                }
                .toImmutableList(),
        )
    }
}

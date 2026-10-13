package eu.kanade.presentation.history

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.HistoryViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.domain.history.model.HistoryWithRelations
import java.util.Date
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class HistoryViewModelStateProvider : PreviewParameterProvider<HistoryViewModel.State> {

    private val multiPage = HistoryViewModel.State(
        searchQuery = null,
        list =
        listOf(HistoryUiModelExamples.headerToday)
            .asSequence()
            .plus(HistoryUiModelExamples.items().take(3))
            .plus(HistoryUiModelExamples.header { it.minus(1.days) })
            .plus(HistoryUiModelExamples.items().take(1))
            .plus(HistoryUiModelExamples.header { it.minus(2.days) })
            .plus(HistoryUiModelExamples.items().take(7))
            .toList(),
        dialog = null,
    )

    private val shortRecent = HistoryViewModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerToday,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val shortFuture = HistoryViewModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerTomorrow,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val empty = HistoryViewModel.State(
        searchQuery = null,
        list = listOf(),
        dialog = null,
    )

    private val loadingWithSearchQuery = HistoryViewModel.State(
        searchQuery = "Example Search Query",
    )

    private val loading = HistoryViewModel.State(
        searchQuery = null,
        list = null,
        dialog = null,
    )

    override val values: Sequence<HistoryViewModel.State> = sequenceOf(
        multiPage,
        shortRecent,
        shortFuture,
        empty,
        loadingWithSearchQuery,
        loading,
    )

    private object HistoryUiModelExamples {
        val headerToday = header()
        val headerTomorrow = HistoryUiModel.Header(
            Clock.System.now().plus(1.days).toLocalDateTime(TimeZone.currentSystemDefault()).date,
        )

        fun header(instantBuilder: (Instant) -> Instant = { it }) =
            HistoryUiModel.Header(
                instantBuilder(Clock.System.now()).toLocalDateTime(TimeZone.currentSystemDefault()).date,
            )

        fun items() = sequence {
            var count = 1
            while (true) {
                yield(randItem { it.copy(visibleTitle = "Example Title $count") })
                count += 1
            }
        }

        fun randItem(historyBuilder: (HistoryUiItem) -> HistoryUiItem = { it }): HistoryUiModel.Item {
            val isManga = Random.nextBoolean()
            val historyItem = if (isManga) {
                HistoryItem.EntryHistory(
                    HistoryWithRelations(
                        id = Random.nextLong(),
                        chapterId = Random.nextLong(),
                        entryId = Random.nextLong(),
                        entryType = EntryType.MANGA,
                        title = "Test Manga",
                        chapterName = "Chapter 1",
                        chapterNumber = Random.nextDouble(),
                        readAt = Date.from(Clock.System.now().toJavaInstant()),
                        readDuration = Random.nextLong(),
                        coverData = randomCover(),
                    ),
                )
            } else {
                HistoryItem.EntryHistory(
                    HistoryWithRelations(
                        id = Random.nextLong(),
                        chapterId = Random.nextLong(),
                        entryId = Random.nextLong(),
                        entryType = EntryType.ANIME,
                        title = "Test Anime",
                        chapterName = "Episode 1",
                        chapterNumber = Random.nextDouble(),
                        readAt = Date.from(Clock.System.now().toJavaInstant()),
                        readDuration = Random.nextLong(),
                        coverData = randomCover(),
                    ),
                )
            }
            val uiItem = HistoryUiItem(
                historyItem = historyItem,
                visibleEntryId = historyItem.entryId,
                visibleTitle = historyItem.entryTitle,
                memberTitle = null,
                visibleCoverData = historyItem.coverData,
            )
            return HistoryUiModel.Item(historyBuilder(uiItem))
        }

        private fun randomCover() = EntryCover(
            entryId = Random.nextLong(),
            sourceId = Random.nextLong(),
            isFavorite = Random.nextBoolean(),
            url = "https://example.com/cover.png",
            lastModified = Random.nextLong(),
        )
    }
}

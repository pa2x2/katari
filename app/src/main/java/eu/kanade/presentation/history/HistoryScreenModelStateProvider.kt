package eu.kanade.presentation.history

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.domain.history.model.HistoryWithRelations
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.random.Random

class HistoryScreenModelStateProvider : PreviewParameterProvider<HistoryScreenModel.State> {

    private val multiPage = HistoryScreenModel.State(
        searchQuery = null,
        list =
        listOf(HistoryUiModelExamples.headerToday)
            .asSequence()
            .plus(HistoryUiModelExamples.items().take(3))
            .plus(HistoryUiModelExamples.header { it.minus(1, ChronoUnit.DAYS) })
            .plus(HistoryUiModelExamples.items().take(1))
            .plus(HistoryUiModelExamples.header { it.minus(2, ChronoUnit.DAYS) })
            .plus(HistoryUiModelExamples.items().take(7))
            .toList(),
        dialog = null,
    )

    private val shortRecent = HistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerToday,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val shortFuture = HistoryScreenModel.State(
        searchQuery = null,
        list = listOf(
            HistoryUiModelExamples.headerTomorrow,
            HistoryUiModelExamples.items().first(),
        ),
        dialog = null,
    )

    private val empty = HistoryScreenModel.State(
        searchQuery = null,
        list = listOf(),
        dialog = null,
    )

    private val loadingWithSearchQuery = HistoryScreenModel.State(
        searchQuery = "Example Search Query",
    )

    private val loading = HistoryScreenModel.State(
        searchQuery = null,
        list = null,
        dialog = null,
    )

    override val values: Sequence<HistoryScreenModel.State> = sequenceOf(
        multiPage,
        shortRecent,
        shortFuture,
        empty,
        loadingWithSearchQuery,
        loading,
    )

    private object HistoryUiModelExamples {
        val headerToday = header()
        val headerTomorrow = HistoryUiModel.Header(LocalDate.now().plusDays(1))

        fun header(instantBuilder: (Instant) -> Instant = { it }) =
            HistoryUiModel.Header(LocalDate.from(instantBuilder(Instant.now())))

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
                        readAt = Date.from(Instant.now()),
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
                        readAt = Date.from(Instant.now()),
                        readDuration = Random.nextLong(),
                        coverData = randomCover(),
                    ),
                )
            }
            val uiItem = HistoryUiItem(
                historyItem = historyItem,
                visibleEntryId = historyItem.entryId,
                visibleTitle = historyItem.entryTitle,
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

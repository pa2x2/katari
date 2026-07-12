package eu.kanade.presentation.history.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.presentation.history.HistoryUiItem
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.history.model.HistoryItem
import tachiyomi.domain.history.model.HistoryWithRelations
import java.util.Date

internal class HistoryItemProvider : PreviewParameterProvider<HistoryUiItem> {

    private val cover = EntryCover(
        entryId = 3L,
        sourceId = 4L,
        isFavorite = false,
        url = "https://example.com/cover.png",
        lastModified = 5L,
    )

    private val mangaHistory = HistoryWithRelations(
        id = 1L,
        chapterId = 2L,
        entryId = 3L,
        entryType = EntryType.MANGA,
        title = "Test Manga",
        chapterName = "Chapter 10",
        chapterNumber = 10.2,
        readAt = Date(1697247357L),
        readDuration = 123L,
        coverData = cover,
    )

    private val animeHistory = HistoryWithRelations(
        id = 4L,
        chapterId = 5L,
        entryId = 6L,
        entryType = EntryType.ANIME,
        title = "Test Anime",
        chapterName = "Episode 1",
        chapterNumber = 1.0,
        readAt = Date(1697247357L),
        readDuration = 456L,
        coverData = cover,
    )

    private val mangaItem = HistoryUiItem(
        historyItem = HistoryItem.EntryHistory(mangaHistory),
        visibleEntryId = mangaHistory.entryId,
        visibleTitle = mangaHistory.title,
        visibleCoverData = mangaHistory.coverData,
    )

    private val animeItem = HistoryUiItem(
        historyItem = HistoryItem.EntryHistory(animeHistory),
        visibleEntryId = animeHistory.entryId,
        visibleTitle = animeHistory.title,
        visibleCoverData = animeHistory.coverData,
    )

    private val mangaWithoutChapterNumber = mangaItem.copy(
        historyItem = HistoryItem.EntryHistory(
            mangaHistory.copy(chapterNumber = -2.0),
        ),
    )

    private val animeWithoutDuration = animeItem.copy(
        historyItem = HistoryItem.EntryHistory(
            animeHistory.copy(readDuration = 0L),
        ),
    )

    override val values: Sequence<HistoryUiItem>
        get() = sequenceOf(mangaItem, animeItem, mangaWithoutChapterNumber, animeWithoutDuration)
}

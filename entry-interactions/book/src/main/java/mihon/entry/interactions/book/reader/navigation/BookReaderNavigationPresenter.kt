package mihon.entry.interactions.book.reader.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.child.EntryChildProgressResult
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class BookReaderNavigationPresentation(
    val chapters: List<EntryChapter>,
    val progressLabels: Map<Long, EntryChildProgressLabel>,
)

internal class BookReaderNavigationPresenter(
    private val getEntryWithChapters: GetEntryWithChapters,
    private val childListFeature: EntryChildListFeature,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(
        entry: Entry,
        readingOrder: List<EntryChapter>,
    ): Flow<BookReaderNavigationPresentation> {
        return getEntryWithChapters.subscribe(entry)
            .map { (_, latestChapters) ->
                val latestById = latestChapters.associateBy(EntryChapter::id)
                readingOrder.map { chapter -> latestById[chapter.id] ?: chapter }
            }
            .distinctUntilChanged()
            .flatMapLatest { chapters ->
                val request = EntryChildProgressRequest(entry = entry, chapters = chapters)
                val labels = when (val result = childListFeature.progressLabels(request)) {
                    is EntryChildProgressResult.Available -> result.labels
                    is EntryChildProgressResult.Inapplicable -> flowOf(emptyMap())
                }
                labels.map { BookReaderNavigationPresentation(chapters, it) }
            }
    }
}

package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class MangaImmersiveMedia(
    val readerChapter: ReaderChapter,
    val initialPageIndex: Int,
    val entry: Entry,
    val child: EntryChapter,
    val activitySession: EntryMediaSessionActivitySession = EntryMediaSessionActivitySession(),
) {
    val pages: List<ReaderPage>
        get() = readerChapter.pages.orEmpty()
}

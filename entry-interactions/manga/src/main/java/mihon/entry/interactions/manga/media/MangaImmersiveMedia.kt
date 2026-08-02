package mihon.entry.interactions.manga.media

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal data class MangaImmersiveMedia(
    val pages: List<MangaImmersivePage>,
    val initialPageIndex: Int,
    val entry: Entry,
    val child: EntryChapter,
)

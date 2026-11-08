package eu.kanade.tachiyomi.ui.video.player

import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildWindow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

/**
 * The episodes before and after [episodeId] in reading order, among those [isShown] keeps, so next, previous and
 * autoplay never land on an episode the entry's list hides. The playing episode keeps its place even when the filters
 * hide it, such as a filler episode opened directly, so its neighbours stay the ones around it.
 */
internal fun List<EntryChapter>.episodeWindow(
    entry: Entry,
    episodeId: Long,
    isShown: (EntryChapter) -> Boolean,
): EntryChildWindow<EntryChapter>? {
    return filter { it.id == episodeId || isShown(it) }
        .sortedForReading(entry)
        .entryChildWindow(episodeId, EntryChapter::id)
}

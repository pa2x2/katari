package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

internal fun mediaSessionContractEvent(
    type: EntryType,
    completed: Boolean = true,
    activity: EntryMediaSessionActivity? = EntryMediaSessionActivity(
        recordedAtEpochMillis = 2_000L,
        durationMillis = 1_000L,
    ),
): EntryMediaSessionEvent.Progressed {
    val entry = Entry.create().copy(id = 91L, source = 17L, type = type)
    val child = EntryChapter.create().copy(
        id = 92L,
        entryId = entry.id,
        url = "/contract-child",
        chapterNumber = 3.0,
    )
    return EntryMediaSessionEvent.Progressed(
        visibleEntry = entry,
        child = child,
        progress = EntryProgressState(
            entryId = entry.id,
            chapterId = child.id,
            resourceKey = child.url,
            locator = EntryProgressLocator(kind = "contract", progression = 0.75),
            completed = completed,
            locatorUpdatedAt = 2_000L,
            completionUpdatedAt = 2_000L,
        ),
        fraction = 0.75,
        activity = activity,
    )
}

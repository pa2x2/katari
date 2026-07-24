package mihon.entry.interactions.manga

import kotlinx.serialization.json.JsonObject
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

internal const val MANGA_PROGRESS_LOCATOR_KIND = "page"

internal val EntryProgressState.pageIndex: Long
    get() = locator.takeIf { it.kind == MANGA_PROGRESS_LOCATOR_KIND }?.position ?: 0L

internal val EntryProgressState.pageCount: Long
    get() = locator.takeIf { it.kind == MANGA_PROGRESS_LOCATOR_KIND }?.extent ?: 0L

internal val EntryProgressState.hasPartialMangaProgress: Boolean
    get() = !completed && pageIndex > 0L

internal val EntryProgressState.lastReadAt: Long
    get() = maxOf(locatorUpdatedAt, completionUpdatedAt)

internal fun mangaProgressState(
    entryId: Long,
    chapterId: Long,
    resourceKey: String,
    pageIndex: Long?,
    pageCount: Long?,
    completed: Boolean,
    locatorUpdatedAt: Long,
    completionUpdatedAt: Long,
): EntryProgressState {
    val safePageIndex = pageIndex?.coerceAtLeast(0L)
    val safePageCount = pageCount?.takeIf { it > 0L }
    return EntryProgressState(
        entryId = entryId,
        chapterId = chapterId,
        resourceKey = resourceKey,
        locator = EntryProgressLocator(
            kind = MANGA_PROGRESS_LOCATOR_KIND,
            position = safePageIndex,
            extent = safePageCount,
            progression = if (safePageIndex != null && safePageCount != null) {
                if (safePageCount == 1L) {
                    1.0
                } else {
                    (safePageIndex.toDouble() / (safePageCount - 1L).toDouble()).coerceIn(0.0, 1.0)
                }
            } else {
                null
            },
            extensions = JsonObject(emptyMap()),
        ),
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt.coerceAtLeast(0L),
        completionUpdatedAt = completionUpdatedAt.coerceAtLeast(0L),
    )
}

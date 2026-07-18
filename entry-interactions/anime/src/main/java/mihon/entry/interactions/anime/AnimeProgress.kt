package mihon.entry.interactions.anime

import kotlinx.serialization.json.JsonObject
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState

internal const val ANIME_PROGRESS_LOCATOR_KIND = "time"

internal val EntryProgressState.positionMs: Long
    get() = locator.takeIf { it.kind == ANIME_PROGRESS_LOCATOR_KIND }?.position ?: 0L

internal val EntryProgressState.durationMs: Long
    get() = locator.takeIf { it.kind == ANIME_PROGRESS_LOCATOR_KIND }?.extent ?: 0L

internal val EntryProgressState.hasPartialAnimeProgress: Boolean
    get() = !completed && positionMs > 0L

internal val EntryProgressState.lastWatchedAt: Long
    get() = maxOf(locatorUpdatedAt, completionUpdatedAt)

internal fun animeProgressState(
    entryId: Long,
    chapterId: Long,
    resourceKey: String,
    positionMs: Long,
    durationMs: Long,
    completed: Boolean,
    locatorUpdatedAt: Long,
    completionUpdatedAt: Long,
): EntryProgressState {
    val safePosition = positionMs.coerceAtLeast(0L)
    val safeDuration = durationMs.takeIf { it > 0L }
    return EntryProgressState(
        entryId = entryId,
        chapterId = chapterId,
        resourceKey = resourceKey,
        locator = EntryProgressLocator(
            kind = ANIME_PROGRESS_LOCATOR_KIND,
            position = safePosition,
            extent = safeDuration,
            progression = safeDuration?.let { (safePosition.toDouble() / it.toDouble()).coerceIn(0.0, 1.0) },
            extensions = JsonObject(emptyMap()),
        ),
        completed = completed,
        locatorUpdatedAt = locatorUpdatedAt.coerceAtLeast(0L),
        completionUpdatedAt = completionUpdatedAt.coerceAtLeast(0L),
    )
}

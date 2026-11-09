package app.katari.entry.interactions.player

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/**
 * Where the desktop app shows the anime player: a screen of the window [open] is called from, which fills it. One
 * player shows at a time, so opening another episode while one plays takes its place.
 */
@PlatformFacility
interface AnimePlayerDestination {
    fun open(context: PlatformContext, request: AnimePlayerRequest)
}

/**
 * The episode a player opens: [episodeId] of [entryId], the entry it was opened from, which [ownerEntryId] owns when a
 * merged entry shows another's episodes. [bypassMerge] keeps the player to [ownerEntryId]'s own episodes. It starts at
 * [startPositionMs] when given, and otherwise where watching left off.
 */
data class AnimePlayerRequest(
    val entryId: Long,
    val episodeId: Long,
    val ownerEntryId: Long,
    val bypassMerge: Boolean,
    val startPositionMs: Long? = null,
)

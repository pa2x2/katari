package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import mihon.entry.interactions.anime.stream.ResolvedStreamCache

internal interface VideoStreamResolver {
    /**
     * The episode's playback options and the stream picked from them. The stream may have no link yet when its source
     * resolves links on demand; [resolveLink] fills it in, so that only the stream about to play costs a request.
     */
    suspend operator fun invoke(
        entryId: Long,
        chapterId: Long,
        ownerEntryId: Long = entryId,
        selection: PlaybackSelection? = null,
    ): ResolveVideoStream.Result

    /**
     * [result] with its stream's link, or the next stream's when that one can't be resolved and the user didn't pick it
     * ([explicitPick]). [resolvedStreams] keeps links for the rest of the player's visit; null resolves every time.
     */
    suspend fun resolveLink(
        result: ResolveVideoStream.Result.Success,
        resolvedStreams: ResolvedStreamCache?,
        explicitPick: Boolean,
    ): ResolveVideoStream.Result
}

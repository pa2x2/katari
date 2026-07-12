package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.PlaybackSelection

internal interface VideoStreamResolver {
    suspend operator fun invoke(
        entryId: Long,
        chapterId: Long,
        ownerEntryId: Long = entryId,
        selection: PlaybackSelection? = null,
    ): ResolveVideoStream.Result
}

package eu.kanade.tachiyomi.ui.video.player.recovery

import eu.kanade.tachiyomi.source.entry.PlaybackSelection

/**
 * Whether the source answered [picked], a dub or server the viewer changed from [listed], with another one. A source
 * takes a selection as a preference and plays something else when the pick lacks the episode, such as a dub that has
 * only a later season, so an answer that differs from the pick means the pick isn't offered for this episode.
 */
internal fun isUnofferedPick(
    listed: PlaybackSelection,
    picked: PlaybackSelection,
    answered: PlaybackSelection,
): Boolean = (picked.dubKey != listed.dubKey && answered.dubKey != picked.dubKey) ||
    (picked.serverKey != listed.serverKey && answered.serverKey != picked.serverKey)

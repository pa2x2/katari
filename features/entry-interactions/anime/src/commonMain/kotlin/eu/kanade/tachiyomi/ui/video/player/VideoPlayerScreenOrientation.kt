package eu.kanade.tachiyomi.ui.video.player

import mihon.entry.interactions.settings.AnimePlayerOrientation

/**
 * Turns the screen the way the orientation setting or the rotate button asks. Only Android has it: a desktop window
 * doesn't turn, so desktop gives the player none and the rotate button doesn't show.
 */
internal interface VideoPlayerOrientationRequests {
    /** [videoAspectRatio] is the video's shape, null until the first frame reports it. */
    fun request(orientation: AnimePlayerOrientation, videoAspectRatio: Float?)
}

/** What the rotate button switches to: the opposite of what the window shows now. */
internal fun rotatedVideoPlayerOrientation(windowIsLandscape: Boolean): AnimePlayerOrientation =
    if (windowIsLandscape) {
        AnimePlayerOrientation.PORTRAIT
    } else {
        AnimePlayerOrientation.LANDSCAPE
    }

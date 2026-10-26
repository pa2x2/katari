package eu.kanade.tachiyomi.ui.video.player.markers

/** The countdown on the skip button before it skips by itself: [progress] runs from 0 to 1, read while drawing. */
internal class VideoPlayerSkipCountdown(val progress: () -> Float, val onDismiss: () -> Unit)

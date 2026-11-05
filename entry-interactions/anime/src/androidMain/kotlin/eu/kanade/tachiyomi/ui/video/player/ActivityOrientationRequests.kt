package eu.kanade.tachiyomi.ui.video.player

import android.app.Activity
import android.content.pm.ActivityInfo
import mihon.entry.interactions.settings.AnimePlayerOrientation

/** Requests the player activity's orientation from the system. */
internal class ActivityOrientationRequests(private val activity: Activity) : VideoPlayerOrientationRequests {
    override fun request(orientation: AnimePlayerOrientation, videoAspectRatio: Float?) {
        val requested = resolveVideoPlayerScreenOrientation(orientation, videoAspectRatio) ?: return
        if (activity.requestedOrientation != requested) {
            activity.requestedOrientation = requested
        }
    }
}

/**
 * The requested orientation for [orientation], or null to keep the current one. Following the video's shape has
 * nothing to go on until the first frame reports its size, and turning the screen before then would rotate it twice
 * for a tall video. Sensor variants let a turned-over phone still flip the picture.
 */
internal fun resolveVideoPlayerScreenOrientation(
    orientation: AnimePlayerOrientation,
    videoAspectRatio: Float?,
): Int? = when (orientation) {
    AnimePlayerOrientation.FOLLOW_VIDEO -> when {
        videoAspectRatio == null -> null
        videoAspectRatio >= 1f -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
    AnimePlayerOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    AnimePlayerOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    AnimePlayerOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}

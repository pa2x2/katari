package eu.kanade.tachiyomi.ui.video.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import mihon.entry.interactions.settings.AnimePlayerOrientation

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

/** What the rotate button switches to: the opposite of what the screen shows now. */
internal fun rotatedVideoPlayerOrientation(configurationOrientation: Int): AnimePlayerOrientation =
    if (configurationOrientation == Configuration.ORIENTATION_LANDSCAPE) {
        AnimePlayerOrientation.PORTRAIT
    } else {
        AnimePlayerOrientation.LANDSCAPE
    }

/** Picture-in-picture windows take the video's shape on their own, so the activity leaves them alone. */
@Composable
internal fun VideoPlayerScreenOrientationEffect(
    activity: Activity,
    orientation: AnimePlayerOrientation,
    videoAspectRatio: Float?,
    isInPictureInPictureMode: Boolean,
) {
    LaunchedEffect(activity, orientation, videoAspectRatio, isInPictureInPictureMode) {
        if (isInPictureInPictureMode) return@LaunchedEffect
        val requested = resolveVideoPlayerScreenOrientation(orientation, videoAspectRatio) ?: return@LaunchedEffect
        if (activity.requestedOrientation != requested) {
            activity.requestedOrientation = requested
        }
    }
}

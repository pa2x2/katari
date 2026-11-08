package eu.kanade.tachiyomi.ui.video.player

import android.content.Context
import android.content.Intent
import mihon.entry.interactions.anime.R

/**
 * The position an intent asks the open player to pause at, which lets the showcase capture a still of an exact frame:
 * a tap on the timeline only aims within a second or so, and pausing a playing film lands wherever it has got to.
 * Only builds that turn on `video_player_accepts_paused_start` read it, and the player is not exported, so the extra
 * reaches it from nothing but the app itself and a root shell.
 */
internal fun Context.pausedStartPositionMs(intent: Intent): Long? {
    if (!resources.getBoolean(R.bool.video_player_accepts_paused_start)) return null
    return intent.getLongExtra(EXTRA_PAUSED_START_POSITION_MS, -1L).takeIf { it >= 0L }
}

private const val EXTRA_PAUSED_START_POSITION_MS = "paused_start_position_ms"

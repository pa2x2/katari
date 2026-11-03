package eu.kanade.tachiyomi.ui.video.player.background

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerActivity
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The player's media session, kept for the whole visit to the player so its notification survives episode changes:
 * each episode's player takes over through [setPlayer]. The seek buttons use the player's own seek increments, and next
 * episode is a custom command because the player only ever holds the current episode.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class VideoPlayerMediaSession(
    private val context: Context,
    player: Player,
    private val onNextEpisode: () -> Unit,
) {
    val session: MediaSession = MediaSession.Builder(context, player)
        .setId(SESSION_ID)
        .setSessionActivity(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, VideoPlayerActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .setCallback(Callback())
        .build()

    fun setPlayer(player: Player, seekStepSeconds: Int, hasNextEpisode: Boolean) {
        session.player = player
        session.setMediaButtonPreferences(
            buildList {
                add(
                    CommandButton.Builder(seekIcon(seekStepSeconds, forward = false))
                        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                        .setDisplayName(
                            context.stringResource(MR.strings.anime_playback_seek_backward, seekStepSeconds),
                        )
                        .setSlots(CommandButton.SLOT_BACK)
                        .build(),
                )
                add(
                    CommandButton.Builder(seekIcon(seekStepSeconds, forward = true))
                        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                        .setDisplayName(context.stringResource(MR.strings.anime_playback_seek_forward, seekStepSeconds))
                        .setSlots(CommandButton.SLOT_FORWARD)
                        .build(),
                )
                if (hasNextEpisode) {
                    add(
                        CommandButton.Builder(CommandButton.ICON_NEXT)
                            .setSessionCommand(NEXT_EPISODE_COMMAND)
                            .setDisplayName(context.stringResource(MR.strings.anime_playback_next_episode))
                            .setSlots(CommandButton.SLOT_OVERFLOW)
                            .build(),
                    )
                }
            },
        )
    }

    fun release() {
        session.release()
    }

    private inner class Callback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val result = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            // Trusted controllers include the media notification and the system's media controls.
            if (controller.isTrusted) {
                result.setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(
                        NEXT_EPISODE_COMMAND,
                    ).build(),
                )
            }
            return result.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand != NEXT_EPISODE_COMMAND) {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
            onNextEpisode()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private companion object {
        const val SESSION_ID = "anime_player"
        val NEXT_EPISODE_COMMAND = SessionCommand("eu.kanade.tachiyomi.ui.video.player.NEXT_EPISODE", Bundle.EMPTY)

        // The steps the player's seek step setting offers each have a matching notification icon.
        fun seekIcon(seconds: Int, forward: Boolean): Int = when (seconds) {
            5 -> if (forward) CommandButton.ICON_SKIP_FORWARD_5 else CommandButton.ICON_SKIP_BACK_5
            10 -> if (forward) CommandButton.ICON_SKIP_FORWARD_10 else CommandButton.ICON_SKIP_BACK_10
            30 -> if (forward) CommandButton.ICON_SKIP_FORWARD_30 else CommandButton.ICON_SKIP_BACK_30
            else -> if (forward) CommandButton.ICON_SKIP_FORWARD else CommandButton.ICON_SKIP_BACK
        }
    }
}

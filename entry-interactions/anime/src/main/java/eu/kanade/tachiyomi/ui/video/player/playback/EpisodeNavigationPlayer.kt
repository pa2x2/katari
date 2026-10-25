package eu.kanade.tachiyomi.ui.video.player.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * The watched player as the player view sees it: previous and next go to the neighbouring episodes, which the player
 * itself doesn't hold.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class EpisodeNavigationPlayer(
    player: Player,
    private val hasPreviousEpisode: Boolean,
    private val hasNextEpisode: Boolean,
    private val onPreviousEpisode: () -> Unit,
    private val onNextEpisode: () -> Unit,
) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands()
            .buildUpon()
            .addIf(Player.COMMAND_SEEK_TO_PREVIOUS, hasPreviousEpisode)
            .addIf(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, hasPreviousEpisode)
            .addIf(Player.COMMAND_SEEK_TO_NEXT, hasNextEpisode)
            .addIf(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, hasNextEpisode)
            .removeIf(Player.COMMAND_SEEK_TO_PREVIOUS, !hasPreviousEpisode)
            .removeIf(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, !hasPreviousEpisode)
            .removeIf(Player.COMMAND_SEEK_TO_NEXT, !hasNextEpisode)
            .removeIf(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, !hasNextEpisode)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            -> hasPreviousEpisode
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            -> hasNextEpisode
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasPreviousMediaItem(): Boolean = hasPreviousEpisode

    override fun hasNextMediaItem(): Boolean = hasNextEpisode

    override fun seekToPreviousMediaItem() {
        if (hasPreviousEpisode) {
            onPreviousEpisode()
        } else {
            super.seekToPreviousMediaItem()
        }
    }

    override fun seekToPrevious() {
        if (hasPreviousEpisode) {
            onPreviousEpisode()
        } else {
            super.seekToPrevious()
        }
    }

    override fun seekToNextMediaItem() {
        if (hasNextEpisode) {
            onNextEpisode()
        } else {
            super.seekToNextMediaItem()
        }
    }

    override fun seekToNext() {
        if (hasNextEpisode) {
            onNextEpisode()
        } else {
            super.seekToNext()
        }
    }
}

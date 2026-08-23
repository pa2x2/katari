package mihon.entry.interactions.anime.media

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoActivePlaybackClock
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerMediaCache
import eu.kanade.tachiyomi.ui.video.player.buildVideoPlayer
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mihon.entry.interactions.media.EntryImmersiveActiveSessionEffect
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeImmersiveRenderer(
    private val handle: EntryImmersiveHandle.Playback,
) : EntryImmersiveRenderer {

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onPagingBlockedChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveProgress) -> Unit,
    ) {
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }
        val mediaCache = remember { Injekt.get<VideoPlayerMediaCache>() }
        val preferences = remember { Injekt.get<AnimePlayerBasePreferences>() }
        val unknownError = stringResource(MR.strings.unknown_error)
        var playerErrorMessage by remember(handle.chapterId, handle.stream.request.url) {
            mutableStateOf<String?>(null)
        }
        var isBuffering by remember(handle.chapterId, handle.stream.request.url) {
            mutableStateOf(false)
        }
        var hasRenderedFirstFrame by remember(handle.chapterId, handle.stream.request.url) {
            mutableStateOf(false)
        }
        val activePlaybackClock = remember(handle.chapterId, handle.stream.request.url) {
            VideoActivePlaybackClock()
        }
        val player = remember(handle.chapterId, handle.stream.request.url, activePlaybackClock) {
            buildVideoPlayer(
                context = context,
                networkHelper = networkHelper,
                mediaCache = mediaCache,
                stream = handle.stream,
                subtitles = handle.subtitles,
            ).also { exoPlayer ->
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            activePlaybackClock.setActive(isPlaying)
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            isBuffering = playbackState == Player.STATE_BUFFERING
                        }

                        override fun onRenderedFirstFrame() {
                            hasRenderedFirstFrame = true
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            isBuffering = false
                            playerErrorMessage = error.message ?: unknownError
                        }
                    },
                )
            }
        }
        var playbackSnapshot by remember(player) { mutableStateOf(player.capturePlaybackSnapshot()) }
        var playIntent by remember(player) { mutableStateOf(true) }
        var muted by remember(player) { mutableStateOf(preferences.immersiveFeedMuted) }
        val audioManager = remember(context) {
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        val latestPlayIntent by rememberUpdatedState(playIntent)

        LaunchedEffect(active) {
            if (active) onPagingBlockedChange(false)
        }
        LaunchedEffect(player, handle.resumePositionMs) {
            if (handle.resumePositionMs > 0L) player.seekTo(handle.resumePositionMs)
            player.playWhenReady = active
            player.prepare()
        }
        LaunchedEffect(player, active, muted) {
            if (active) {
                val storedMuted = preferences.immersiveFeedMuted
                if (storedMuted != muted) {
                    muted = storedMuted
                    return@LaunchedEffect
                }
            }
            player.volume = if (active && !muted) 1f else 0f
            if (active && playIntent) {
                player.play()
            } else if (!active) {
                player.pause()
            }
        }
        DisposableEffect(active, audioManager) {
            if (!active) return@DisposableEffect onDispose {}

            var previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (shouldUnmuteAfterVolumeChange(muted, previousVolume, currentVolume)) {
                        muted = false
                        preferences.immersiveFeedMuted = false
                    }
                    previousVolume = currentVolume
                }
            }
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver,
            )
            onDispose { context.contentResolver.unregisterContentObserver(volumeObserver) }
        }
        LifecycleStartEffect(player, active) {
            if (active && latestPlayIntent) player.play()
            onStopOrDispose { player.pause() }
        }
        LaunchedEffect(player) {
            while (isActive) {
                playbackSnapshot = player.capturePlaybackSnapshot()
                delay(PLAYBACK_SNAPSHOT_INTERVAL_MS)
            }
        }
        LaunchedEffect(player, handle.chapterId, active) {
            if (!active) return@LaunchedEffect
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                val snapshot = player.capturePlaybackSnapshot()
                onProgress(
                    EntryImmersiveProgress.Playback(
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        activeDurationMs = activePlaybackClock.checkpoint(),
                    ),
                )
            }
        }
        EntryImmersiveActiveSessionEffect(active, onPagingBlockedChange) {
            val snapshot = player.capturePlaybackSnapshot()
            onProgress(
                EntryImmersiveProgress.Playback(
                    positionMs = snapshot.positionMs,
                    durationMs = snapshot.durationMs,
                    activeDurationMs = activePlaybackClock.checkpoint(),
                    resetSession = true,
                ),
            )
            player.pause()
            player.seekTo(0L)
            playIntent = true
            playbackSnapshot = player.capturePlaybackSnapshot()
        }
        DisposableEffect(player) {
            onDispose {
                val snapshot = player.capturePlaybackSnapshot()
                onProgress(
                    EntryImmersiveProgress.Playback(
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        activeDurationMs = activePlaybackClock.checkpoint(),
                    ),
                )
                player.stop()
                player.release()
            }
        }

        AnimeImmersivePlayerSurface(
            player = player,
            active = active,
            controlsVisible = controlsVisible,
            controlsBottomInset = controlsBottomInset,
            playbackSnapshot = playbackSnapshot,
            isBuffering = isBuffering,
            hasRenderedFirstFrame = hasRenderedFirstFrame,
            playerErrorMessage = playerErrorMessage,
            muted = muted,
            onToggleControls = onToggleControls,
            onPlaybackSnapshotChange = { playbackSnapshot = it },
            onPlayIntentChange = { playIntent = it },
            onMutedChange = {
                muted = it
                preferences.immersiveFeedMuted = it
            },
            onClearError = { playerErrorMessage = null },
            modifier = modifier,
        )
    }
}

internal fun shouldUnmuteAfterVolumeChange(
    muted: Boolean,
    previousVolume: Int,
    currentVolume: Int,
): Boolean = muted && currentVolume > previousVolume

private const val PLAYBACK_SNAPSHOT_INTERVAL_MS = 250L
private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L

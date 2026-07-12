package mihon.entry.interactions.anime

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.video.player.AnimePlayerBasePreferences
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerMediaCache
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerPlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.buildVideoPlayer
import eu.kanade.tachiyomi.ui.video.player.capturePlaybackSnapshot
import eu.kanade.tachiyomi.ui.video.player.coerceToPlaybackDuration
import eu.kanade.tachiyomi.ui.video.player.formatPlaybackTimestamp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryImmersiveFeedHandle
import mihon.entry.interactions.EntryImmersiveFeedProgress
import mihon.entry.interactions.EntryImmersiveFeedRenderer
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class AnimeImmersiveFeedRenderer(
    private val handle: EntryImmersiveFeedHandle.Playback,
) : EntryImmersiveFeedRenderer {

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content(
        modifier: Modifier,
        active: Boolean,
        controlsVisible: Boolean,
        controlsBottomInset: Dp,
        onToggleControls: () -> Unit,
        onZoomStateChange: (Boolean) -> Unit,
        onProgress: (EntryImmersiveFeedProgress) -> Unit,
    ) {
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }
        val mediaCache = remember { Injekt.get<VideoPlayerMediaCache>() }
        val preferences = remember { Injekt.get<AnimePlayerBasePreferences>() }
        val unknownError = stringResource(MR.strings.unknown_error)
        var playerErrorMessage by remember(handle.chapterId, handle.stream.request.url) {
            mutableStateOf<String?>(null)
        }
        var hasRenderedFirstFrame by remember(handle.chapterId, handle.stream.request.url) {
            mutableStateOf(false)
        }
        val player = remember(handle.chapterId, handle.stream.request.url) {
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
                        override fun onRenderedFirstFrame() {
                            hasRenderedFirstFrame = true
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            playerErrorMessage = error.message ?: unknownError
                        }
                    },
                )
            }
        }
        var playbackSnapshot by remember(player) { mutableStateOf(player.capturePlaybackSnapshot()) }
        var speedBoostActive by remember(player) { mutableStateOf(false) }
        var playIntent by remember(player) { mutableStateOf(true) }
        var muted by remember(player) { mutableStateOf(preferences.immersiveFeedMuted.get()) }
        val audioManager = remember(context) {
            context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        val latestPlayIntent by rememberUpdatedState(playIntent)
        val videoAlpha by animateFloatAsState(
            targetValue = if (hasRenderedFirstFrame) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "immersiveVideoAlpha",
        )

        LaunchedEffect(active) {
            if (active) onZoomStateChange(false)
        }
        LaunchedEffect(player, handle.resumePositionMs) {
            if (handle.resumePositionMs > 0L) player.seekTo(handle.resumePositionMs)
            player.playWhenReady = active
            player.prepare()
        }
        LaunchedEffect(player, active, muted) {
            if (active) {
                val storedMuted = preferences.immersiveFeedMuted.get()
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
                        preferences.immersiveFeedMuted.set(false)
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
                onProgress(EntryImmersiveFeedProgress.Playback(snapshot.positionMs, snapshot.durationMs))
            }
        }
        DisposableEffect(player, handle.chapterId, active) {
            onDispose {
                if (active) {
                    val snapshot = player.capturePlaybackSnapshot()
                    onProgress(
                        EntryImmersiveFeedProgress.Playback(
                            positionMs = snapshot.positionMs,
                            durationMs = snapshot.durationMs,
                            resetSession = true,
                        ),
                    )
                    player.pause()
                    player.seekTo(0L)
                    playIntent = true
                    playbackSnapshot = player.capturePlaybackSnapshot()
                    onZoomStateChange(false)
                }
            }
        }
        DisposableEffect(player) {
            onDispose {
                player.stop()
                player.release()
            }
        }

        Box(
            modifier = modifier.background(Color.Black.copy(alpha = videoAlpha)),
        ) {
            AndroidView(
                factory = { androidContext ->
                    PlayerView(androidContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                        setKeepContentOnPlayerReset(true)
                        setEnableComposeSurfaceSyncWorkaround(true)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = player
                    playerView.setKeepContentOnPlayerReset(true)
                    playerView.useController = false
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(videoAlpha),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(player) {
                        detectTapGestures(
                            onPress = {
                                coroutineScope {
                                    var boosted = false
                                    val boostJob = launch {
                                        delay(SPEED_BOOST_PRESS_DELAY_MS)
                                        player.setPlaybackSpeed(SPEED_BOOST_MULTIPLIER)
                                        boosted = true
                                        speedBoostActive = true
                                    }
                                    try {
                                        val released = tryAwaitRelease()
                                        if (!boosted && released) {
                                            onToggleControls()
                                        }
                                    } finally {
                                        boostJob.cancel()
                                        if (boosted) {
                                            player.setPlaybackSpeed(NORMAL_PLAYBACK_SPEED)
                                            speedBoostActive = false
                                            playbackSnapshot = player.capturePlaybackSnapshot()
                                        }
                                    }
                                }
                            },
                        )
                    },
            )

            if (speedBoostActive) {
                Text(
                    text = "2x",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 84.dp)
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            if (active && controlsVisible) {
                AnimeImmersiveFeedTimeline(
                    snapshot = playbackSnapshot,
                    onSeek = { positionMs ->
                        player.seekTo(positionMs)
                        if (playbackSnapshot.playbackEnded) player.play()
                        playIntent = true
                        playbackSnapshot = player.capturePlaybackSnapshot()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = controlsBottomInset + 4.dp),
                )
                IconButton(
                    onClick = {
                        if (playbackSnapshot.playbackEnded) player.seekTo(0L)
                        if (player.isPlaying) {
                            player.pause()
                            playIntent = false
                        } else {
                            player.play()
                            playIntent = true
                        }
                        playbackSnapshot = player.capturePlaybackSnapshot()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.48f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (playbackSnapshot.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (playbackSnapshot.isPlaying) MR.strings.action_pause else MR.strings.action_play,
                            ),
                            modifier = Modifier.size(28.dp),
                            tint = Color.White,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        muted = !muted
                        preferences.immersiveFeedMuted.set(muted)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 12.dp,
                            bottom = controlsBottomInset + 36.dp,
                        )
                        .size(48.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.48f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (muted) {
                                Icons.AutoMirrored.Outlined.VolumeOff
                            } else {
                                Icons.AutoMirrored.Outlined.VolumeUp
                            },
                            contentDescription = stringResource(
                                if (muted) MR.strings.action_unmute else MR.strings.action_mute,
                            ),
                            modifier = Modifier.size(20.dp),
                            tint = Color.White,
                        )
                    }
                }
            }

            playerErrorMessage?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.64f), MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = message, color = Color.White)
                    Button(
                        onClick = {
                            playerErrorMessage = null
                            playIntent = true
                            player.prepare()
                            player.play()
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(stringResource(MR.strings.action_retry))
                    }
                }
            }
        }
    }
}

internal fun shouldUnmuteAfterVolumeChange(
    muted: Boolean,
    previousVolume: Int,
    currentVolume: Int,
): Boolean = muted && currentVolume > previousVolume

@Composable
private fun AnimeImmersiveFeedTimeline(
    snapshot: VideoPlayerPlaybackSnapshot,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationMs = snapshot.durationMs
    var isScrubbing by remember(durationMs) { mutableStateOf(false) }
    var scrubPositionMs by remember(durationMs) { mutableStateOf(snapshot.positionMs) }
    val displayedPositionMs = if (isScrubbing) scrubPositionMs else snapshot.positionMs
    val playedFraction = if (durationMs > 0L) displayedPositionMs.toFloat() / durationMs else 0f
    val bufferedFraction = if (durationMs > 0L) snapshot.bufferedPositionMs.toFloat() / durationMs else 0f
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val updateScrubPosition: (Float) -> Unit = { x ->
            if (durationMs > 0L && widthPx > 0f) {
                scrubPositionMs = (durationMs * (x / widthPx).coerceIn(0f, 1f)).toLong()
                    .coerceToPlaybackDuration(durationMs)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isScrubbing) {
                Text(
                    text = "${formatPlaybackTimestamp(scrubPositionMs)} / ${formatPlaybackTimestamp(durationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(durationMs, widthPx) {
                        if (durationMs <= 0L) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                updateScrubPosition(offset.x)
                            },
                            onDrag = { change, _ -> updateScrubPosition(change.position.x) },
                            onDragEnd = {
                                onSeek(scrubPositionMs)
                                isScrubbing = false
                            },
                            onDragCancel = { isScrubbing = false },
                        )
                    },
            ) {
                val centerY = size.height / 2f
                val trackStroke = if (isScrubbing) 5.dp.toPx() else 3.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.28f),
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                    strokeWidth = trackStroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.42f),
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width * bufferedFraction.coerceIn(0f, 1f), centerY),
                    strokeWidth = trackStroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White,
                    start = androidx.compose.ui.geometry.Offset(0f, centerY),
                    end = androidx.compose.ui.geometry.Offset(size.width * playedFraction.coerceIn(0f, 1f), centerY),
                    strokeWidth = trackStroke,
                    cap = StrokeCap.Round,
                )
                if (isScrubbing) {
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            x = size.width * playedFraction.coerceIn(0f, 1f),
                            y = centerY,
                        ),
                    )
                }
            }
        }
    }
}

private const val PLAYBACK_SNAPSHOT_INTERVAL_MS = 250L
private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
private const val SPEED_BOOST_PRESS_DELAY_MS = 350L
private const val SPEED_BOOST_MULTIPLIER = 2f
private const val NORMAL_PLAYBACK_SPEED = 1f

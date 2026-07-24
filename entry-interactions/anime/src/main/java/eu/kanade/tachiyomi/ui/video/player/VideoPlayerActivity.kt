package eu.kanade.tachiyomi.ui.video.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerErrorOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerLoadingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerNextEpisodeOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSheet
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSwitchingOverlay
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleEditorOverlay
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.anime.R
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.viewer.settings.ViewerSettingBinder
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

private const val SIDE_GESTURE_VERTICAL_RATIO = 1.35f
private const val SIDE_GESTURE_ZONE_HORIZONTAL_INSET_FRACTION = 0.1f
private const val SIDE_GESTURE_ZONE_WIDTH_FRACTION = 0.16f
private const val SIDE_GESTURE_ZONE_HEIGHT_FRACTION = 0.58f
private const val MIN_GESTURE_LEVEL = 0
private const val MAX_GESTURE_LEVEL = 100
private const val DEFAULT_GESTURE_LEVEL = 50
private const val DEFAULT_SYSTEM_BRIGHTNESS = 127
private const val SYSTEM_BRIGHTNESS_MAX = 255
private const val MIN_POSITIVE_BRIGHTNESS = 0.01f
private const val BRIGHTNESS_RESPONSE_GAMMA = 2.2f
private const val NEXT_EPISODE_COUNTDOWN_VISIBLE_BEFORE_END_MS = 5_000L
private const val NEXT_EPISODE_COUNTDOWN_DURATION_MS = 8_000L
private const val NEXT_EPISODE_COUNTDOWN_TICK_MS = 33L
private const val TEMPORARY_SPEED_BOOST = 2f
private const val HOLD_SPEED_ZONE_START_FRACTION = 0.25f
private const val HOLD_SPEED_ZONE_END_FRACTION = 0.75f
private const val HOLD_SPEED_ZONE_TOP_FRACTION = 0.18f
private const val HOLD_SPEED_ZONE_BOTTOM_FRACTION = 0.78f
private const val HOLD_SPEED_TOP_ZONE_LEFT_FRACTION = 0.1f
private const val HOLD_SPEED_TOP_ZONE_RIGHT_FRACTION = 0.9f
private const val HOLD_SPEED_TOP_ZONE_TOP_FRACTION = 0f
private const val HOLD_SPEED_TOP_ZONE_BOTTOM_FRACTION = 0.2f
private const val SEEK_PREVIEW_FIRST_DEBOUNCE_MS = 50L
private const val SEEK_PREVIEW_THROTTLE_MS = 200L
private const val SEEK_PREVIEW_MIN_DELTA_MS = 2_000L
private const val SEEK_PREVIEW_MAX_WIDTH = 320
private const val SEEK_PREVIEW_MAX_HEIGHT = 180
private const val MEDIA_SESSION_ID = "anime_player"

private fun PlaybackException.toDetailedPlaybackMessage(): String {
    val directCause = cause
    val rootCause = generateSequence(directCause) { it.cause }.lastOrNull()
    return buildList {
        add(errorCodeName)
        message?.takeIf { it.isNotBlank() }?.let(::add)
        directCause?.formatForPlaybackError()?.let(::add)
        rootCause
            ?.takeIf { it !== directCause }
            ?.formatForPlaybackError(prefix = "Caused by")
            ?.let(::add)
    }
        .distinct()
        .joinToString(separator = "\n")
        .ifBlank { "Playback error" }
}

private fun Throwable.formatForPlaybackError(prefix: String = "Cause"): String {
    return listOfNotNull(
        prefix,
        this::class.simpleName,
        message?.takeIf { it.isNotBlank() },
    ).joinToString(": ")
}

class VideoPlayerActivity : AppCompatActivity() {

    private val viewModel by viewModels<VideoPlayerViewModel>()
    private val networkHelper: NetworkHelper by lazy { Injekt.get() }
    private val mediaCache: VideoPlayerMediaCache by lazy { Injekt.get() }
    private val animePlayerPreferences: AnimePlayerPreferences by lazy { Injekt.get() }
    private val viewerSettingBinder: ViewerSettingBinder by lazy { Injekt.get() }
    private val pictureInPictureSetting by lazy {
        viewerSettingBinder.bind(animePlayerPreferences.pictureInPictureSetting)
    }
    private val seekPreviewSetting by lazy {
        viewerSettingBinder.bind(animePlayerPreferences.seekPreviewSetting)
    }
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val windowInsetsController by lazy { WindowInsetsControllerCompat(window, window.decorView) }
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var mediaSession: MediaSession? = null
    private var progressSaveJob: Job? = null
    private var supportsPictureInPicture = false
    private var pictureInPictureEnabled by mutableStateOf(false)
    private var isInPictureInPictureModeState by mutableStateOf(false)
    private var pendingPictureInPictureOnPause = false
    private var latestPlaybackSnapshot = VideoPlayerPlaybackSnapshot()
    private var playbackBrightnessLevel by mutableStateOf(DEFAULT_GESTURE_LEVEL)
    private var playbackBrightnessOverlayValue by mutableStateOf(0)
    private var playbackVolumeLevel by mutableStateOf(0)
    private var playbackVolumeMaxLevel by mutableStateOf(1)
    private var controlsLocked by mutableStateOf(false)
    private var pictureInPictureActionReceiverRegistered = false
    private var brightnessObserverRegistered = false
    private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            if (isUsingSystemBrightness()) {
                syncPlaybackBrightnessState()
            }
        }
    }
    private val pictureInPictureActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PICTURE_IN_PICTURE_TOGGLE_PLAYBACK -> togglePlaybackFromPictureInPicture()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        registerAnimePlayerSecureScreen()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hideSystemUi()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)

        supportsPictureInPicture = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        pictureInPictureEnabled = pictureInPictureSetting.resolveProfile().effectiveValue
        isInPictureInPictureModeState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInPictureInPictureMode
        } else {
            false
        }
        syncPlaybackBrightnessState()
        syncPlaybackVolumeState()
        registerBrightnessObserverIfNeeded()
        registerPictureInPictureActionReceiverIfNeeded()

        val animeId = intent.extras?.getLong(EXTRA_VIDEO_ID, INVALID_ID) ?: INVALID_ID
        val ownerAnimeId = intent.extras?.getLong(EXTRA_OWNER_VIDEO_ID, animeId) ?: animeId
        val episodeId = intent.extras?.getLong(EXTRA_EPISODE_ID, INVALID_ID) ?: INVALID_ID
        val bypassMerge = intent.extras?.getBoolean(EXTRA_BYPASS_MERGE, false) ?: false
        if (animeId == INVALID_ID || episodeId == INVALID_ID) {
            finish()
            return
        }
        viewModel.init(animeId, episodeId, ownerAnimeId, bypassMerge)

        setPlayerComposeContent {
            val state by viewModel.state.collectAsState()

            VideoPlayerScaffold(
                state = state,
                networkHelper = networkHelper,
            )
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    override fun onResume() {
        super.onResume()
        pictureInPictureEnabled = pictureInPictureSetting.resolveProfile().effectiveValue
        pendingPictureInPictureOnPause = false
        syncPlaybackBrightnessState()
        syncPlaybackVolumeState(player)
        hideSystemUi()
    }

    @Composable
    private fun VideoPlayerScaffold(
        state: VideoPlayerViewModel.State,
        networkHelper: NetworkHelper,
    ) {
        HideSystemUiEffect()
        VideoPlayerScreen(
            state = state,
            networkHelper = networkHelper,
        )
    }

    @Composable
    @OptIn(markerClass = [UnstableApi::class])
    private fun VideoPlayerScreen(
        state: VideoPlayerViewModel.State,
        networkHelper: NetworkHelper,
    ) {
        when (val current = state) {
            VideoPlayerViewModel.State.Loading -> {
                VideoPlayerLoadingOverlay(modifier = Modifier.fillMaxSize())
            }
            is VideoPlayerViewModel.State.Ready -> {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is VideoPlayerViewModel.Event.ShowMessage -> toast(event.message)
                            is VideoPlayerViewModel.Event.ShowPreviewMessage -> toast(event.message)
                        }
                    }
                }
                val resolvedSeekPreview by seekPreviewSetting.state.collectAsState()
                val seekPreviewEnabled = resolvedSeekPreview.effectiveValue

                val subtitlePayloadKey = remember(current.playback.subtitles) {
                    current.playback.subtitles.joinToString(separator = "||") { subtitleChoiceKey(it) }
                }
                var controlsVisible by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(true) }
                var startupOverlayVisible by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(true) }
                var settingsVisible by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(false) }
                var episodesDrawerVisible by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(false) }
                var subtitleEditorVisible by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(false) }
                var subtitleEditorDraft by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(current.playback.subtitleAppearance)
                }
                var resumePlaybackAfterSubtitleEditor by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(false) }
                var isScrubbing by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(false) }
                var scrubPositionMs by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(current.resumePositionMs.coerceAtLeast(0L))
                }
                var playbackSnapshot by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(
                        VideoPlayerPlaybackSnapshot(
                            positionMs = current.resumePositionMs.coerceAtLeast(0L),
                        ),
                    )
                }
                var controllerInteractionSequence by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(0L) }
                var seekFeedbackSequence by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(0L) }
                var seekFeedbackState by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf<VideoPlayerSeekFeedbackState?>(null)
                }
                var ignoreNextGestureSeekTapUp by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(false)
                }
                var sideGestureFeedbackSequence by remember(
                    current.chapterId,
                    current.streamUrl,
                    subtitlePayloadKey,
                ) { mutableStateOf(0L) }
                var sideGestureFeedbackState by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf<VideoPlayerSideGestureFeedbackState?>(null)
                }
                var seekPreviewState by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(VideoPlayerSeekPreviewState())
                }
                var seekPreviewAspectRatio by remember(
                    current.chapterId,
                    current.streamUrl,
                    current.playbackRevision,
                    seekPreviewEnabled,
                ) {
                    mutableStateOf<Float?>(null)
                }
                var seekPreviewFailed by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(false)
                }
                var playerErrorMessage by remember(
                    current.chapterId,
                    current.streamUrl,
                    current.playbackRevision,
                    subtitlePayloadKey,
                ) {
                    mutableStateOf<String?>(null)
                }
                var lastSeekPreviewRequestAtMs by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf(0L)
                }
                var lastSeekPreviewPositionMs by remember(current.chapterId, current.streamUrl, subtitlePayloadKey) {
                    mutableStateOf<Long?>(null)
                }
                var nextEpisodeCountdownProgress by remember { mutableStateOf(0f) }
                var nextEpisodeCountdownStartedAtMs by remember { mutableStateOf<Long?>(null) }
                var nextEpisodeAdvanceInFlight by remember { mutableStateOf(false) }
                var temporarySpeedBoostActive by remember { mutableStateOf(false) }
                val isInPictureInPictureMode = isInPictureInPictureModeState
                val shouldHideChromeForSeekFeedback = seekFeedbackState?.hidePlayerChrome == true
                val hidePlayerChrome = shouldHideChromeForSeekFeedback || isInPictureInPictureMode
                val nextEpisodeAvailable = current.nextChapterId != null
                val playbackRemainingMs = (playbackSnapshot.durationMs - playbackSnapshot.positionMs).coerceAtLeast(0L)
                val shouldShowNextEpisodeCountdown =
                    playbackSnapshot.durationMs > 0L &&
                        playbackRemainingMs <= NEXT_EPISODE_COUNTDOWN_VISIBLE_BEFORE_END_MS
                val showNextEpisodeCta = nextEpisodeCountdownStartedAtMs != null &&
                    nextEpisodeAvailable &&
                    !nextEpisodeAdvanceInFlight &&
                    !controlsVisible &&
                    !isInPictureInPictureMode &&
                    !subtitleEditorVisible
                val showPictureInPictureButton =
                    pictureInPictureEnabled && supportsPictureInPicture &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                val effectivePlaybackSpeed = if (temporarySpeedBoostActive) {
                    TEMPORARY_SPEED_BOOST
                } else {
                    current.playback.sessionPlaybackSpeed
                }
                val onPreviousEpisode = {
                    temporarySpeedBoostActive = false
                    controlsVisible = !controlsLocked
                    settingsVisible = false
                    episodesDrawerVisible = false
                    subtitleEditorVisible = false
                    isScrubbing = false
                    nextEpisodeCountdownStartedAtMs = null
                    nextEpisodeAdvanceInFlight = true
                    nextEpisodeCountdownProgress = 0f
                    controllerInteractionSequence += 1L
                    flushPlaybackState()
                    viewModel.playPreviousEpisode()
                }
                val onNextEpisode = {
                    if (!nextEpisodeAdvanceInFlight && nextEpisodeAvailable) {
                        temporarySpeedBoostActive = false
                        nextEpisodeCountdownStartedAtMs = null
                        nextEpisodeAdvanceInFlight = true
                        nextEpisodeCountdownProgress = 0f
                        controlsVisible = !controlsLocked
                        settingsVisible = false
                        episodesDrawerVisible = false
                        subtitleEditorVisible = false
                        isScrubbing = false
                        controllerInteractionSequence += 1L
                        flushPlaybackState()
                        viewModel.playNextEpisode()
                    }
                }
                val latestPlayback by rememberUpdatedState(current.playback)
                val latestTemporarySpeedBoostActive by rememberUpdatedState(temporarySpeedBoostActive)
                val initialSettingsDraft = remember(current.playback) { current.playback.toSettingsDraft() }
                val currentPlayer =
                    remember(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
                        buildVideoPlayer(
                            context = context,
                            networkHelper = networkHelper,
                            mediaCache = mediaCache,
                            stream = current.stream,
                            subtitles = current.playback.subtitles,
                        ).also { exoPlayer ->
                            exoPlayer.addListener(
                                object : Player.Listener {
                                    override fun onPositionDiscontinuity(
                                        oldPosition: Player.PositionInfo,
                                        newPosition: Player.PositionInfo,
                                        reason: Int,
                                    ) {
                                        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                                            viewModel.resetPlaybackBaseline(newPosition.positionMs)
                                        }
                                        scrubPositionMs = newPosition.positionMs.coerceAtLeast(0L)
                                        playbackSnapshot = exoPlayer.capturePlaybackSnapshot()
                                        latestPlaybackSnapshot = playbackSnapshot
                                        updatePictureInPictureParams(playbackSnapshot)
                                    }

                                    override fun onRenderedFirstFrame() {
                                        startupOverlayVisible = false
                                    }

                                    override fun onPlayerError(error: PlaybackException) {
                                        startupOverlayVisible = false
                                        playerErrorMessage = error.toDetailedPlaybackMessage()
                                        logcat(LogPriority.ERROR, error) {
                                            "VideoPlayer: playback failed episodeId=${current.chapterId} " +
                                                "stream=${current.streamLabel} code=${error.errorCodeName}"
                                        }
                                    }

                                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                                        val latestPlaybackState = latestPlayback
                                        exoPlayer.applySubtitleSelection(latestPlaybackState.currentSubtitle)
                                        viewModel.updateAdaptiveQualities(exoPlayer.availableAdaptiveQualities())
                                        viewModel.updateSubtitleOptions(
                                            exoPlayer.availableSubtitleTracks(latestPlaybackState.subtitles),
                                        )
                                        val resolvedSubtitleSelection = exoPlayer.resolveAppliedSubtitleSelection(
                                            latestPlaybackState.currentSubtitle,
                                            latestPlaybackState.subtitles,
                                        )
                                        if (resolvedSubtitleSelection != latestPlaybackState.currentSubtitle) {
                                            viewModel.updateResolvedSubtitle(resolvedSubtitleSelection)
                                        }
                                        playbackSnapshot = exoPlayer.capturePlaybackSnapshot()
                                        latestPlaybackSnapshot = playbackSnapshot
                                        updatePictureInPictureParams(playbackSnapshot)
                                    }

                                    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                                        syncPlaybackVolumeState(exoPlayer, deviceInfo)
                                    }

                                    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
                                        syncPlaybackVolumeState(exoPlayer)
                                    }

                                    override fun onEvents(player: Player, events: Player.Events) {
                                        playbackSnapshot = exoPlayer.capturePlaybackSnapshot()
                                        latestPlaybackSnapshot = playbackSnapshot
                                        updatePictureInPictureParams(playbackSnapshot)
                                    }
                                },
                            )
                            if (current.resumePositionMs > 0L) {
                                exoPlayer.seekTo(current.resumePositionMs)
                            }
                            playbackSnapshot = exoPlayer.capturePlaybackSnapshot()
                            latestPlaybackSnapshot = playbackSnapshot
                            updatePictureInPictureParams(playbackSnapshot)
                        }
                    }
                val controllerPlayer = remember(
                    currentPlayer,
                    current.previousChapterId,
                    current.nextChapterId,
                ) {
                    EpisodeNavigationPlayer(
                        player = currentPlayer,
                        hasPreviousEpisode = current.previousChapterId != null,
                        hasNextEpisode = current.nextChapterId != null,
                        onPreviousEpisode = onPreviousEpisode,
                        onNextEpisode = onNextEpisode,
                    )
                }

                val registerControllerInteraction: (Boolean) -> Unit = { shouldShowControls ->
                    if (shouldShowControls) {
                        controlsVisible = true
                    }
                    controllerInteractionSequence += 1L
                }
                val triggerSeekFeedback: (VideoPlayerSeekDirection, Boolean) -> Unit = { direction, hidePlayerChrome ->
                    seekFeedbackSequence += 1L
                    val now = System.currentTimeMillis()
                    val previousState = seekFeedbackState
                    val isBurstContinuation = previousState != null &&
                        previousState.direction == direction &&
                        now - previousState.updatedAtMillis <= SEEK_FEEDBACK_BURST_WINDOW_MS
                    seekFeedbackState = VideoPlayerSeekFeedbackState(
                        direction = direction,
                        totalSeconds = if (isBurstContinuation) {
                            previousState.totalSeconds + (SEEK_INCREMENT_MS / 1000L).toInt()
                        } else {
                            (SEEK_INCREMENT_MS / 1000L).toInt()
                        },
                        hidePlayerChrome = hidePlayerChrome,
                        sequence = seekFeedbackSequence,
                        updatedAtMillis = now,
                    )
                }
                val triggerSideGestureFeedback: (
                    VideoPlayerSideGestureType,
                    Int,
                    Int,
                ) -> Unit = { type, level, maxLevel ->
                    sideGestureFeedbackSequence += 1L
                    sideGestureFeedbackState = VideoPlayerSideGestureFeedbackState(
                        type = type,
                        level = level,
                        maxLevel = maxLevel,
                        sequence = sideGestureFeedbackSequence,
                    )
                }
                val applyBrightnessLevel: (Int) -> Unit = { level ->
                    val normalizedLevel = level.coerceIn(MIN_GESTURE_LEVEL, MAX_GESTURE_LEVEL)
                    if (playbackBrightnessLevel != normalizedLevel) {
                        playbackBrightnessLevel = normalizedLevel
                    }
                    val overlayValue = brightnessOverlayLevelFor(normalizedLevel)
                    if (playbackBrightnessOverlayValue != overlayValue) {
                        playbackBrightnessOverlayValue = overlayValue
                    }
                    applyPlaybackBrightnessToWindow(normalizedLevel)
                    triggerSideGestureFeedback(
                        VideoPlayerSideGestureType.Brightness,
                        normalizedLevel,
                        MAX_GESTURE_LEVEL,
                    )
                }
                val applyVolumeLevel: (Int) -> Unit = { level ->
                    val maxLevel = playbackVolumeMaxLevel.coerceAtLeast(1)
                    val normalizedLevel = level.coerceIn(0, maxLevel)
                    if (playbackVolumeLevel != normalizedLevel) {
                        playbackVolumeLevel = normalizedLevel
                    }
                    if (currentPlayer.isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)) {
                        currentPlayer.setDeviceVolume(normalizedLevel, 0)
                    } else if (currentPlayer.canSetDeviceVolumeCompat()) {
                        currentPlayer.setDeviceVolumeCompat(normalizedLevel)
                    } else {
                        val streamMaxVolume = audioManager.safeMusicStreamMaxVolume()
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            normalizedLevel.coerceIn(0, streamMaxVolume),
                            0,
                        )
                    }
                    triggerSideGestureFeedback(
                        VideoPlayerSideGestureType.Volume,
                        normalizedLevel,
                        maxLevel,
                    )
                }
                val seekBy: (Long) -> Unit = { deltaMs ->
                    val durationMs = playbackSnapshot.durationMs.takeIf { it > 0L }
                        ?: currentPlayer.duration.coerceAtLeast(0L)
                    val basePositionMs = if (isScrubbing) scrubPositionMs else currentPlayer.currentPosition
                    val targetPositionMs = (basePositionMs + deltaMs).coerceToPlaybackDuration(durationMs)

                    isScrubbing = false
                    scrubPositionMs = targetPositionMs
                    playbackSnapshot = playbackSnapshot.copy(positionMs = targetPositionMs)
                    currentPlayer.seekTo(targetPositionMs)
                }
                val resolveSeekDirectionFromTap: (Float, Int) -> VideoPlayerSeekDirection? = { tapX, width ->
                    when {
                        width <= 0 -> null
                        tapX <= width / 3f -> VideoPlayerSeekDirection.Backward
                        tapX >= width * 2f / 3f -> VideoPlayerSeekDirection.Forward
                        else -> null
                    }
                }
                val performGestureSeek: (VideoPlayerSeekDirection) -> Unit = { direction ->
                    controlsVisible = false
                    registerControllerInteraction(false)
                    seekBy(
                        if (direction == VideoPlayerSeekDirection.Backward) {
                            -SEEK_INCREMENT_MS
                        } else {
                            SEEK_INCREMENT_MS
                        },
                    )
                    triggerSeekFeedback(direction, true)
                }
                val togglePlayback = {
                    registerControllerInteraction(true)
                    if (playbackSnapshot.playbackEnded) {
                        currentPlayer.seekTo(0L)
                    }
                    if (currentPlayer.isPlaying) {
                        currentPlayer.pause()
                    } else {
                        currentPlayer.play()
                    }
                }

                LaunchedEffect(current.chapterId, current.streamUrl, current.playbackRevision, subtitlePayloadKey) {
                    playerErrorMessage = null
                    startupOverlayVisible = true
                    settingsVisible = false
                    episodesDrawerVisible = false
                    subtitleEditorVisible = false
                    subtitleEditorDraft = current.playback.subtitleAppearance
                    resumePlaybackAfterSubtitleEditor = false
                    temporarySpeedBoostActive = false
                    nextEpisodeCountdownStartedAtMs = null
                    nextEpisodeCountdownProgress = 0f
                    nextEpisodeAdvanceInFlight = false
                    controlsVisible = !isInPictureInPictureMode && !controlsLocked
                    isScrubbing = false
                    ignoreNextGestureSeekTapUp = false
                    seekFeedbackState = null
                    sideGestureFeedbackState = null
                    syncPlaybackBrightnessState()
                    applyPlaybackBrightnessToWindow(
                        if (isUsingSystemBrightness()) DEFAULT_GESTURE_LEVEL else playbackBrightnessLevel,
                    )
                    syncPlaybackVolumeState(currentPlayer)
                    scrubPositionMs = current.resumePositionMs.coerceAtLeast(0L)
                    playbackSnapshot = VideoPlayerPlaybackSnapshot(positionMs = scrubPositionMs)
                    controllerInteractionSequence += 1L
                    releasePlayer(persistState = false)
                    player = currentPlayer
                    mediaSession = MediaSession.Builder(context, currentPlayer)
                        .setId(MEDIA_SESSION_ID)
                        .build()
                    startProgressSaves(currentPlayer)
                    currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
                    currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
                    currentPlayer.playWhenReady = true
                    currentPlayer.prepare()
                    playbackSnapshot = currentPlayer.capturePlaybackSnapshot()
                    latestPlaybackSnapshot = playbackSnapshot
                    updatePictureInPictureParams(playbackSnapshot)
                }

                LaunchedEffect(current.playback.currentAdaptiveQuality, currentPlayer) {
                    currentPlayer.applyAdaptiveQuality(current.playback.currentAdaptiveQuality)
                }

                LaunchedEffect(current.playback.currentSubtitle, currentPlayer) {
                    currentPlayer.applySubtitleSelection(current.playback.currentSubtitle)
                }

                LaunchedEffect(effectivePlaybackSpeed, currentPlayer) {
                    currentPlayer.setPlaybackSpeed(effectivePlaybackSpeed)
                }

                LaunchedEffect(currentPlayer) {
                    while (isActive) {
                        playbackSnapshot = currentPlayer.capturePlaybackSnapshot()
                        latestPlaybackSnapshot = playbackSnapshot
                        updatePictureInPictureParams(playbackSnapshot)
                        if (!isScrubbing) {
                            scrubPositionMs = playbackSnapshot.positionMs
                        }
                        delay(
                            if (playbackSnapshot.isPlaying || playbackSnapshot.isLoading || isScrubbing) {
                                PLAYBACK_SNAPSHOT_INTERVAL_MS
                            } else {
                                PAUSED_PLAYBACK_SNAPSHOT_INTERVAL_MS
                            },
                        )
                    }
                }
                val seekPreviewPlayer = remember(
                    current.chapterId,
                    current.streamUrl,
                    current.playbackRevision,
                    seekPreviewEnabled,
                ) {
                    if (!seekPreviewEnabled) {
                        null
                    } else {
                        buildVideoPlayer(
                            context = context,
                            networkHelper = networkHelper,
                            mediaCache = mediaCache,
                            stream = current.stream,
                            subtitles = emptyList(),
                        ).also { exoPlayer ->
                            exoPlayer.volume = 0f
                            exoPlayer.playWhenReady = false
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .setMaxVideoSize(SEEK_PREVIEW_MAX_WIDTH, SEEK_PREVIEW_MAX_HEIGHT)
                                .build()
                            exoPlayer.addListener(
                                object : Player.Listener {
                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        if (playbackState == Player.STATE_READY) {
                                            seekPreviewState = seekPreviewState.copy(
                                                loading = false,
                                                available = true,
                                            )
                                        }
                                    }

                                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                                        seekPreviewAspectRatio = videoSize.displayAspectRatioOrNull()
                                    }

                                    override fun onPlayerError(error: PlaybackException) {
                                        seekPreviewFailed = true
                                        seekPreviewState = VideoPlayerSeekPreviewState()
                                    }
                                },
                            )
                        }
                    }
                }
                DisposableEffect(seekPreviewPlayer) {
                    onDispose {
                        seekPreviewPlayer?.release()
                    }
                }

                LaunchedEffect(
                    controlsVisible,
                    controllerInteractionSequence,
                    playbackSnapshot.isPlaying,
                    playbackSnapshot.isLoading,
                    isScrubbing,
                    settingsVisible,
                    episodesDrawerVisible,
                    startupOverlayVisible,
                ) {
                    if (
                        !controlsVisible ||
                        !playbackSnapshot.isPlaying ||
                        playbackSnapshot.isLoading ||
                        isScrubbing ||
                        settingsVisible ||
                        episodesDrawerVisible ||
                        startupOverlayVisible
                    ) {
                        return@LaunchedEffect
                    }

                    delay(CONTROLS_AUTO_HIDE_DELAY_MS)
                    controlsVisible = false
                }

                val displayedPositionMs = if (isScrubbing) {
                    scrubPositionMs.coerceToPlaybackDuration(playbackSnapshot.durationMs)
                } else {
                    playbackSnapshot.positionMs
                }
                LaunchedEffect(
                    seekPreviewEnabled,
                    seekPreviewPlayer,
                    isScrubbing,
                    scrubPositionMs,
                    playbackSnapshot.durationMs,
                    isInPictureInPictureMode,
                    controlsLocked,
                    startupOverlayVisible,
                    current.isSourceSwitching,
                    seekPreviewFailed,
                ) {
                    val previewPlayer = seekPreviewPlayer
                    val durationMs = playbackSnapshot.durationMs
                    if (
                        !seekPreviewEnabled ||
                        previewPlayer == null ||
                        !isScrubbing ||
                        durationMs <= 0L ||
                        isInPictureInPictureMode ||
                        controlsLocked ||
                        startupOverlayVisible ||
                        current.isSourceSwitching ||
                        seekPreviewFailed
                    ) {
                        seekPreviewState = VideoPlayerSeekPreviewState()
                        return@LaunchedEffect
                    }

                    val targetPositionMs = scrubPositionMs.coerceToPlaybackDuration(durationMs)
                    val lastPreviewPosition = lastSeekPreviewPositionMs
                    if (
                        lastPreviewPosition != null &&
                        abs(targetPositionMs - lastPreviewPosition) < SEEK_PREVIEW_MIN_DELTA_MS
                    ) {
                        return@LaunchedEffect
                    }

                    val now = SystemClock.elapsedRealtime()
                    val delayMs = if (lastSeekPreviewRequestAtMs == 0L) {
                        SEEK_PREVIEW_FIRST_DEBOUNCE_MS
                    } else {
                        (SEEK_PREVIEW_THROTTLE_MS - (now - lastSeekPreviewRequestAtMs)).coerceAtLeast(0L)
                    }
                    delay(delayMs)

                    val latestTargetPositionMs = scrubPositionMs.coerceToPlaybackDuration(durationMs)
                    val latestLastPreviewPosition = lastSeekPreviewPositionMs
                    if (
                        latestLastPreviewPosition != null &&
                        abs(latestTargetPositionMs - latestLastPreviewPosition) < SEEK_PREVIEW_MIN_DELTA_MS
                    ) {
                        return@LaunchedEffect
                    }

                    seekPreviewState = VideoPlayerSeekPreviewState(
                        positionMs = latestTargetPositionMs,
                        visible = true,
                        loading = true,
                        available = seekPreviewState.available,
                    )
                    lastSeekPreviewRequestAtMs = SystemClock.elapsedRealtime()
                    lastSeekPreviewPositionMs = latestTargetPositionMs
                    previewPlayer.seekTo(latestTargetPositionMs)
                    if (previewPlayer.playbackState == Player.STATE_IDLE) {
                        previewPlayer.prepare()
                    }
                }
                val latestSettingsVisible by rememberUpdatedState(settingsVisible)
                val latestControlsVisible by rememberUpdatedState(controlsVisible)
                val latestControlsLocked by rememberUpdatedState(controlsLocked)
                val latestRegisterControllerInteraction by rememberUpdatedState(registerControllerInteraction)
                val latestResolveSeekDirectionFromTap by rememberUpdatedState(resolveSeekDirectionFromTap)
                val latestPerformGestureSeek by rememberUpdatedState(performGestureSeek)
                val latestSeekGestureModeActive by rememberUpdatedState(shouldHideChromeForSeekFeedback)
                val latestSpeedBoostEligibility by rememberUpdatedState(
                    !controlsVisible &&
                        !controlsLocked &&
                        !settingsVisible &&
                        !subtitleEditorVisible &&
                        !shouldHideChromeForSeekFeedback,
                )
                BackHandler(enabled = episodesDrawerVisible) {
                    episodesDrawerVisible = false
                    registerControllerInteraction(true)
                }
                LaunchedEffect(isInPictureInPictureMode) {
                    if (isInPictureInPictureMode) {
                        temporarySpeedBoostActive = false
                        controlsVisible = false
                        settingsVisible = false
                        episodesDrawerVisible = false
                        subtitleEditorVisible = false
                        isScrubbing = false
                        ignoreNextGestureSeekTapUp = false
                        seekFeedbackState = null
                        sideGestureFeedbackState = null
                    } else {
                        hideSystemUi()
                    }
                }
                LaunchedEffect(
                    shouldShowNextEpisodeCountdown,
                    nextEpisodeAvailable,
                    playbackSnapshot.playbackEnded,
                    playbackRemainingMs,
                    subtitleEditorVisible,
                    isInPictureInPictureMode,
                    nextEpisodeAdvanceInFlight,
                ) {
                    if (!nextEpisodeAvailable || nextEpisodeAdvanceInFlight || subtitleEditorVisible) {
                        nextEpisodeCountdownStartedAtMs = null
                        nextEpisodeCountdownProgress = 0f
                        return@LaunchedEffect
                    }

                    if (
                        nextEpisodeCountdownStartedAtMs != null &&
                        !playbackSnapshot.playbackEnded &&
                        playbackRemainingMs > NEXT_EPISODE_COUNTDOWN_VISIBLE_BEFORE_END_MS
                    ) {
                        nextEpisodeCountdownStartedAtMs = null
                        nextEpisodeCountdownProgress = 0f
                        return@LaunchedEffect
                    }

                    if (
                        nextEpisodeCountdownStartedAtMs == null &&
                        shouldShowNextEpisodeCountdown &&
                        !isInPictureInPictureMode
                    ) {
                        nextEpisodeCountdownStartedAtMs = SystemClock.elapsedRealtime()
                        viewModel.preloadNextEpisode()
                    }
                }

                LaunchedEffect(nextEpisodeCountdownStartedAtMs, current.nextChapterId) {
                    val startedAt = nextEpisodeCountdownStartedAtMs ?: run {
                        nextEpisodeCountdownProgress = 0f
                        return@LaunchedEffect
                    }

                    nextEpisodeCountdownProgress = 0f
                    while (isActive && !nextEpisodeAdvanceInFlight && nextEpisodeCountdownStartedAtMs == startedAt) {
                        val elapsed = SystemClock.elapsedRealtime() - startedAt
                        val progress = (elapsed / NEXT_EPISODE_COUNTDOWN_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                        nextEpisodeCountdownProgress = progress
                        if (progress >= 1f) {
                            onNextEpisode()
                            break
                        }
                        delay(NEXT_EPISODE_COUNTDOWN_TICK_MS)
                    }
                }
                LaunchedEffect(
                    playbackSnapshot.playbackEnded,
                    current.nextChapterId,
                    isInPictureInPictureMode,
                    subtitleEditorVisible,
                ) {
                    if (
                        playbackSnapshot.playbackEnded &&
                        current.nextChapterId != null &&
                        isInPictureInPictureMode &&
                        !subtitleEditorVisible &&
                        !nextEpisodeAdvanceInFlight
                    ) {
                        onNextEpisode()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        factory = { androidContext ->
                            PlayerView(androidContext).apply {
                                player = controllerPlayer
                                setKeepContentOnPlayerReset(true)
                                useController = false
                                setEnableComposeSurfaceSyncWorkaround(true)
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                                setBackgroundColor(android.graphics.Color.BLACK)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                isClickable = true
                                isFocusable = true
                            }
                        },
                        update = { playerView ->
                            playerView.player = controllerPlayer
                            playerView.setKeepContentOnPlayerReset(true)
                            playerView.useController = false
                            playerView.applySubtitleAppearance(
                                appearance = if (subtitleEditorVisible) {
                                    subtitleEditorDraft
                                } else {
                                    current.playback.subtitleAppearance
                                },
                                editorVisible = subtitleEditorVisible,
                            )
                            val touchSlop = ViewConfiguration.get(playerView.context).scaledTouchSlop
                            var sideGestureState: SideGestureState? = null
                            var holdSpeedGestureState: HoldSpeedGestureState? = null
                            val gestureDetector = GestureDetector(
                                playerView.context,
                                object : GestureDetector.SimpleOnGestureListener() {
                                    override fun onDown(e: MotionEvent): Boolean = true

                                    override fun onLongPress(e: MotionEvent) {
                                        val currentHoldState = holdSpeedGestureState ?: return
                                        if (
                                            currentHoldState.cancelled ||
                                            currentHoldState.longPressTriggered ||
                                            !latestSpeedBoostEligibility
                                        ) {
                                            return
                                        }
                                        if (
                                            isTouchInHoldSpeedZone(
                                                touchX = currentHoldState.startX,
                                                touchY = currentHoldState.startY,
                                                playerWidth = playerView.width,
                                                playerHeight = playerView.height,
                                            )
                                        ) {
                                            holdSpeedGestureState = currentHoldState.copy(longPressTriggered = true)
                                            temporarySpeedBoostActive = true
                                            latestRegisterControllerInteraction(false)
                                        }
                                    }

                                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                        if (latestTemporarySpeedBoostActive) {
                                            return true
                                        }
                                        if (latestControlsLocked) {
                                            if (latestControlsVisible) {
                                                controlsVisible = false
                                            } else {
                                                latestRegisterControllerInteraction(true)
                                            }
                                            return true
                                        }
                                        if (!latestSettingsVisible && !subtitleEditorVisible) {
                                            if (latestSeekGestureModeActive) {
                                                return true
                                            }
                                            if (latestControlsVisible) {
                                                controlsVisible = false
                                            } else {
                                                latestRegisterControllerInteraction(true)
                                            }
                                        }
                                        return true
                                    }

                                    override fun onDoubleTap(e: MotionEvent): Boolean {
                                        if (latestTemporarySpeedBoostActive) {
                                            return true
                                        }
                                        if (latestControlsLocked) {
                                            return true
                                        }
                                        if (!latestSettingsVisible && !subtitleEditorVisible) {
                                            val direction = latestResolveSeekDirectionFromTap(e.x, playerView.width)
                                            if (direction != null) {
                                                ignoreNextGestureSeekTapUp = true
                                                latestPerformGestureSeek(direction)
                                                return true
                                            }
                                        }
                                        return false
                                    }
                                },
                            )
                            playerView.setOnTouchListener { _, motionEvent ->
                                if (subtitleEditorVisible) {
                                    return@setOnTouchListener true
                                }

                                if (latestControlsLocked) {
                                    val handled = gestureDetector.onTouchEvent(motionEvent)
                                    if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                                        playerView.performClick()
                                    }
                                    return@setOnTouchListener handled
                                }

                                when (motionEvent.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        temporarySpeedBoostActive = false
                                        sideGestureState = SideGestureState(
                                            startX = motionEvent.x,
                                            startY = motionEvent.y,
                                            initialLevel = 0,
                                        )
                                        holdSpeedGestureState = HoldSpeedGestureState(
                                            startX = motionEvent.x,
                                            startY = motionEvent.y,
                                        )
                                    }

                                    MotionEvent.ACTION_MOVE -> {
                                        val currentHoldState = holdSpeedGestureState
                                        if (currentHoldState != null) {
                                            val movedBeyondSlop =
                                                abs(motionEvent.x - currentHoldState.startX) > touchSlop ||
                                                    abs(motionEvent.y - currentHoldState.startY) > touchSlop
                                            if (
                                                currentHoldState.longPressTriggered &&
                                                !isTouchInHoldSpeedZone(
                                                    touchX = motionEvent.x,
                                                    touchY = motionEvent.y,
                                                    playerWidth = playerView.width,
                                                    playerHeight = playerView.height,
                                                )
                                            ) {
                                                temporarySpeedBoostActive = false
                                                holdSpeedGestureState = currentHoldState.copy(
                                                    cancelled = true,
                                                    longPressTriggered = false,
                                                )
                                            } else if (!currentHoldState.longPressTriggered && movedBeyondSlop) {
                                                holdSpeedGestureState = currentHoldState.copy(cancelled = true)
                                            }
                                        }
                                        val currentSideGestureState = sideGestureState
                                        if (
                                            currentSideGestureState != null &&
                                            !latestSettingsVisible &&
                                            !latestSeekGestureModeActive
                                        ) {
                                            val deltaX = motionEvent.x - currentSideGestureState.startX
                                            val deltaY = motionEvent.y - currentSideGestureState.startY
                                            if (
                                                !currentSideGestureState.active &&
                                                abs(deltaY) > touchSlop &&
                                                abs(deltaY) > abs(deltaX) * SIDE_GESTURE_VERTICAL_RATIO
                                            ) {
                                                val gestureType = resolveSideGestureType(
                                                    startX = currentSideGestureState.startX,
                                                    startY = currentSideGestureState.startY,
                                                    playerWidth = playerView.width,
                                                    playerHeight = playerView.height,
                                                )
                                                if (gestureType != null) {
                                                    sideGestureState = currentSideGestureState.copy(
                                                        type = gestureType,
                                                        initialLevel = if (gestureType ==
                                                            VideoPlayerSideGestureType.Brightness
                                                        ) {
                                                            playbackBrightnessLevel
                                                        } else {
                                                            playbackVolumeLevel
                                                        },
                                                        active = true,
                                                    )
                                                }
                                            }

                                            val activeSideGestureState = sideGestureState
                                            if (activeSideGestureState?.active == true) {
                                                val dragFraction = verticalGestureFraction(
                                                    startY = activeSideGestureState.startY,
                                                    currentY = motionEvent.y,
                                                    playerWidth = playerView.width,
                                                    playerHeight = playerView.height,
                                                )
                                                when (activeSideGestureState.type) {
                                                    VideoPlayerSideGestureType.Brightness -> {
                                                        val targetLevel = activeSideGestureState.initialLevel +
                                                            (dragFraction * MAX_GESTURE_LEVEL).toInt()
                                                        applyBrightnessLevel(targetLevel)
                                                    }

                                                    VideoPlayerSideGestureType.Volume -> {
                                                        val targetLevel = activeSideGestureState.initialLevel +
                                                            (
                                                                dragFraction * playbackVolumeMaxLevel.coerceAtLeast(
                                                                    1,
                                                                )
                                                                ).toInt()
                                                        applyVolumeLevel(targetLevel)
                                                    }

                                                    null -> Unit
                                                }
                                                controlsVisible = false
                                                latestRegisterControllerInteraction(false)
                                                return@setOnTouchListener true
                                            }
                                        }
                                    }

                                    MotionEvent.ACTION_UP,
                                    MotionEvent.ACTION_CANCEL,
                                    -> {
                                        temporarySpeedBoostActive = false
                                        holdSpeedGestureState = null
                                        if (sideGestureState?.active == true) {
                                            sideGestureState = null
                                            if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                                                playerView.performClick()
                                            }
                                            return@setOnTouchListener true
                                        }
                                        sideGestureState = null
                                    }
                                }

                                val handled = gestureDetector.onTouchEvent(motionEvent)
                                if (latestTemporarySpeedBoostActive) {
                                    return@setOnTouchListener true
                                }
                                if (
                                    !latestSettingsVisible &&
                                    latestSeekGestureModeActive &&
                                    motionEvent.actionMasked == MotionEvent.ACTION_UP
                                ) {
                                    val direction = latestResolveSeekDirectionFromTap(motionEvent.x, playerView.width)
                                    if (direction != null) {
                                        if (ignoreNextGestureSeekTapUp) {
                                            ignoreNextGestureSeekTapUp = false
                                        } else {
                                            latestPerformGestureSeek(direction)
                                        }
                                        return@setOnTouchListener true
                                    }
                                }
                                if (motionEvent.actionMasked == MotionEvent.ACTION_UP && !handled) {
                                    playerView.performClick()
                                }
                                handled
                            }
                        },
                    )

                    VideoPlayerContentOverlay(
                        brightness = playbackBrightnessOverlayValue,
                        color = null,
                        colorBlendMode = null,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (!isInPictureInPictureMode && !subtitleEditorVisible && !startupOverlayVisible) {
                        VideoPlayerOverlay(
                            visible = controlsVisible,
                            locked = controlsLocked,
                            videoTitle = current.chapterTitle,
                            episodeName = current.chapterName,
                            playbackSnapshot = playbackSnapshot,
                            displayedPositionMs = displayedPositionMs,
                            isScrubbing = isScrubbing,
                            episodesDrawerVisible = episodesDrawerVisible,
                            anime = current.entry,
                            currentEpisodeId = current.chapterId,
                            episodeListItems = current.chapterListItems,
                            playbackStateByEpisodeId = current.playbackStateByChapterId,
                            sourceAvailable = current.sourceAvailable,
                            hasPreviousEpisode = current.previousChapterId != null,
                            hasNextEpisode = current.nextChapterId != null,
                            seekFeedbackState = seekFeedbackState,
                            seekPreviewState = seekPreviewState,
                            seekPreviewPlayer = seekPreviewPlayer,
                            seekPreviewAspectRatio = seekPreviewAspectRatio,
                            sideGestureFeedbackState = sideGestureFeedbackState,
                            hideChromeForSeekFeedback = hidePlayerChrome,
                            onSeekFeedbackDismissed = {
                                ignoreNextGestureSeekTapUp = false
                                seekFeedbackState = null
                            },
                            onSideGestureFeedbackDismissed = {
                                sideGestureFeedbackState = null
                            },
                            onBack = ::finish,
                            showPictureInPictureButton = showPictureInPictureButton,
                            onEnterPictureInPicture = ::enterPictureInPictureFromControls,
                            onToggleLock = {
                                temporarySpeedBoostActive = false
                                controlsLocked = !controlsLocked
                                controlsVisible = true
                                settingsVisible = false
                                episodesDrawerVisible = false
                                isScrubbing = false
                                registerControllerInteraction(true)
                            },
                            onOpenSettings = {
                                temporarySpeedBoostActive = false
                                episodesDrawerVisible = false
                                settingsVisible = true
                                registerControllerInteraction(true)
                            },
                            onOpenEpisodes = {
                                temporarySpeedBoostActive = false
                                settingsVisible = false
                                episodesDrawerVisible = true
                                registerControllerInteraction(true)
                            },
                            onDismissEpisodes = {
                                episodesDrawerVisible = false
                                registerControllerInteraction(true)
                            },
                            onEpisodeSelected = { episode: EntryChapter ->
                                if (episode.id != current.chapterId) {
                                    temporarySpeedBoostActive = false
                                    episodesDrawerVisible = false
                                    controlsVisible = !controlsLocked
                                    flushPlaybackState()
                                    viewModel.playEpisode(
                                        visibleEntryId = current.visibleEntryId,
                                        ownerEntryId = episode.entryId,
                                        episodeId = episode.id,
                                    )
                                }
                            },
                            onPreviousEpisode = onPreviousEpisode,
                            onSeekBackward = {
                                registerControllerInteraction(true)
                                seekBy(-SEEK_INCREMENT_MS)
                                triggerSeekFeedback(VideoPlayerSeekDirection.Backward, false)
                            },
                            onTogglePlayback = togglePlayback,
                            onSeekForward = {
                                registerControllerInteraction(true)
                                seekBy(SEEK_INCREMENT_MS)
                                triggerSeekFeedback(VideoPlayerSeekDirection.Forward, false)
                            },
                            onNextEpisode = onNextEpisode,
                            onScrubStarted = {
                                registerControllerInteraction(true)
                                isScrubbing = true
                                scrubPositionMs = playbackSnapshot.positionMs
                                seekPreviewState = if (
                                    seekPreviewEnabled &&
                                    !seekPreviewFailed &&
                                    playbackSnapshot.durationMs > 0L
                                ) {
                                    VideoPlayerSeekPreviewState(
                                        positionMs = playbackSnapshot.positionMs,
                                        visible = true,
                                        loading = true,
                                    )
                                } else {
                                    VideoPlayerSeekPreviewState()
                                }
                            },
                            onScrubPositionChange = { positionMs ->
                                val targetPositionMs = positionMs.coerceToPlaybackDuration(playbackSnapshot.durationMs)
                                scrubPositionMs = targetPositionMs
                                val lastPreviewPosition = lastSeekPreviewPositionMs
                                if (
                                    seekPreviewEnabled &&
                                    !seekPreviewFailed &&
                                    playbackSnapshot.durationMs > 0L &&
                                    (
                                        lastPreviewPosition == null ||
                                            abs(targetPositionMs - lastPreviewPosition) >= SEEK_PREVIEW_MIN_DELTA_MS
                                        )
                                ) {
                                    seekPreviewState = seekPreviewState.copy(
                                        positionMs = targetPositionMs,
                                        visible = true,
                                        loading = true,
                                    )
                                }
                            },
                            onScrubFinished = {
                                val targetPositionMs = scrubPositionMs.coerceToPlaybackDuration(
                                    playbackSnapshot.durationMs,
                                )
                                isScrubbing = false
                                seekPreviewState = VideoPlayerSeekPreviewState()
                                scrubPositionMs = targetPositionMs
                                playbackSnapshot = playbackSnapshot.copy(positionMs = targetPositionMs)
                                latestPlaybackSnapshot = playbackSnapshot
                                currentPlayer.seekTo(targetPositionMs)
                                registerControllerInteraction(true)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    VideoPlayerNextEpisodeOverlay(
                        visible = showNextEpisodeCta,
                        progress = nextEpisodeCountdownProgress,
                        onClick = onNextEpisode,
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (startupOverlayVisible && !isInPictureInPictureMode && !subtitleEditorVisible) {
                        VideoPlayerLoadingOverlay(modifier = Modifier.fillMaxSize())
                    }

                    if (current.isSourceSwitching && !isInPictureInPictureMode && !subtitleEditorVisible) {
                        VideoPlayerSwitchingOverlay(modifier = Modifier.align(Alignment.TopCenter))
                    }

                    if (playerErrorMessage != null && !isInPictureInPictureMode && !subtitleEditorVisible) {
                        VideoPlayerErrorOverlay(
                            message = playerErrorMessage!!,
                            onBack = ::finish,
                            onRetry = {
                                viewModel.retryCurrentPlayback(playbackSnapshot.positionMs)
                            },
                            onSettings = {
                                settingsVisible = true
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (settingsVisible && !isInPictureInPictureMode && !subtitleEditorVisible) {
                        VideoPlayerSettingsSheet(
                            playback = current.playback,
                            onDismissRequest = {
                                temporarySpeedBoostActive = false
                                settingsVisible = false
                                registerControllerInteraction(true)
                            },
                            onApplySettings = { draft ->
                                temporarySpeedBoostActive = false
                                persistPlayback(currentPlayer)
                                if (draft.resetToDefaults) {
                                    viewModel.resetPlayerSettings()
                                } else {
                                    if (draft.playbackSpeed != initialSettingsDraft.playbackSpeed) {
                                        viewModel.updateSessionPlaybackSpeed(draft.playbackSpeed)
                                    }
                                    if (draft.adaptiveQuality != initialSettingsDraft.adaptiveQuality) {
                                        viewModel.selectAdaptiveQuality(draft.adaptiveQuality)
                                    }
                                    if (draft.subtitleSelection != initialSettingsDraft.subtitleSelection) {
                                        viewModel.selectSubtitle(draft.subtitleSelection)
                                    }
                                    if (draft.sourceSelection != initialSettingsDraft.sourceSelection) {
                                        viewModel.applySourceSelection(draft.sourceSelection)
                                    }
                                }
                            },
                            onPreviewSourceSelection = viewModel::previewSourceSelection,
                            onPreviewDefaultSourceSelection = viewModel::previewDefaultSourceSelection,
                            onOpenSubtitleSettings = {
                                temporarySpeedBoostActive = false
                                settingsVisible = false
                                episodesDrawerVisible = false
                                subtitleEditorDraft = current.playback.subtitleAppearance
                                resumePlaybackAfterSubtitleEditor = currentPlayer.isPlaying
                                currentPlayer.pause()
                                subtitleEditorVisible = true
                            },
                        )
                    }

                    if (subtitleEditorVisible && !isInPictureInPictureMode) {
                        VideoSubtitleEditorOverlay(
                            draftAppearance = subtitleEditorDraft,
                            previewCues = currentPlayer.subtitlePreviewCues().ifEmpty {
                                listOf(
                                    Cue.Builder()
                                        .setText(stringResource(MR.strings.anime_playback_subtitle_sample))
                                        .build(),
                                )
                            },
                            previewText = currentPlayer.subtitlePreviewText()
                                ?: stringResource(MR.strings.anime_playback_subtitle_sample),
                            onDraftChange = { subtitleEditorDraft = it.normalized() },
                            onDismissRequest = {
                                temporarySpeedBoostActive = false
                                subtitleEditorVisible = false
                                if (resumePlaybackAfterSubtitleEditor) {
                                    currentPlayer.play()
                                }
                            },
                            onReset = { subtitleEditorDraft = VideoSubtitleAppearance() },
                            onDone = {
                                temporarySpeedBoostActive = false
                                val normalizedAppearance = subtitleEditorDraft.normalized()
                                subtitleEditorVisible = false
                                viewModel.updateSubtitleAppearance(normalizedAppearance)
                                if (resumePlaybackAfterSubtitleEditor) {
                                    currentPlayer.play()
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            is VideoPlayerViewModel.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "Unable to open video",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = current.message,
                            color = Color.White.copy(alpha = 0.84f),
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 24.dp),
                        ) {
                            TextButton(onClick = ::finish) {
                                Text(
                                    text = stringResource(MR.strings.action_cancel),
                                    color = Color.White,
                                )
                            }
                            Button(onClick = viewModel::retry) {
                                Text(text = stringResource(MR.strings.action_retry))
                            }
                        }
                        IconButton(
                            onClick = ::finish,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        flushPlaybackState()
        super.onPause()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val handled = super.dispatchKeyEvent(event)
        if (
            event.action == android.view.KeyEvent.ACTION_UP &&
            (
                event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
                    event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
                    event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE
                )
        ) {
            syncPlaybackVolumeState(player)
        }
        return handled
    }

    override fun onUserLeaveHint() {
        pendingPictureInPictureOnPause = canAutoEnterPictureInPicture()
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            pendingPictureInPictureOnPause &&
            !enterPictureInPictureIfAvailable()
        ) {
            pendingPictureInPictureOnPause = false
        }
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPictureInPictureModeState = isInPictureInPictureMode
        pendingPictureInPictureOnPause = false
        if (!isInPictureInPictureMode) {
            hideSystemUi()
        }
    }

    override fun onStop() {
        if (!isInPictureInPictureModeState && !pendingPictureInPictureOnPause) {
            pendingPictureInPictureOnPause = false
            player?.pause()
        }
        flushPlaybackState()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterBrightnessObserver()
        unregisterPictureInPictureActionReceiver()
        resetPlaybackBrightness()
        releasePlayer(persistState = false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun startProgressSaves(player: ExoPlayer) {
        progressSaveJob?.cancel()
        progressSaveJob = lifecycleScope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                persistPlayback(player)
            }
        }
    }

    private fun flushPlaybackState() {
        player?.let(::persistPlayback)
    }

    private fun persistPlayback(player: ExoPlayer) {
        viewModel.persistPlayback(
            positionMs = player.currentPosition,
            durationMs = player.duration.takeIf { it > 0L } ?: 0L,
        )
    }

    private fun releasePlayer(persistState: Boolean = true) {
        progressSaveJob?.cancel()
        progressSaveJob = null
        if (persistState) {
            flushPlaybackState()
        }
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
    }

    @Composable
    private fun HideSystemUiEffect() {
        LaunchedEffect(Unit) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun syncPlaybackBrightnessState() {
        val screenBrightness = window.attributes.screenBrightness
        if (screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            playbackBrightnessLevel = readSystemBrightnessLevel()
            playbackBrightnessOverlayValue = 0
        } else {
            playbackBrightnessLevel = gestureLevelForScreenBrightness(screenBrightness)
            playbackBrightnessOverlayValue = brightnessOverlayLevelFor(playbackBrightnessLevel)
        }
    }

    private fun applyPlaybackBrightnessToWindow(level: Int) {
        val normalizedLevel = level.coerceIn(MIN_GESTURE_LEVEL, MAX_GESTURE_LEVEL)
        val screenBrightness = when {
            isUsingSystemBrightness(level) -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            normalizedLevel > 0 -> screenBrightnessForGestureLevel(normalizedLevel)
            else -> MIN_POSITIVE_BRIGHTNESS
        }
        window.attributes = window.attributes.apply {
            this.screenBrightness = screenBrightness
        }
    }

    private fun readSystemBrightnessLevel(): Int {
        val brightnessValue = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(DEFAULT_SYSTEM_BRIGHTNESS)
        return gestureLevelForScreenBrightness(brightnessValue / SYSTEM_BRIGHTNESS_MAX.toFloat())
    }

    private fun syncPlaybackVolumeState(
        player: ExoPlayer? = null,
        deviceInfo: DeviceInfo? = player?.deviceInfo,
    ) {
        val currentPlayer = player ?: this.player
        val currentDeviceInfo = deviceInfo ?: currentPlayer?.deviceInfo
        val maxLevel = currentDeviceInfo?.maxVolume
            ?.takeIf { it > 0 }
            ?: audioManager.safeMusicStreamMaxVolume()
        playbackVolumeMaxLevel = maxLevel.coerceAtLeast(1)
        playbackVolumeLevel = when {
            currentPlayer != null && currentPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) -> {
                currentPlayer.getDeviceVolume().coerceIn(0, playbackVolumeMaxLevel)
            }
            else -> {
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    .coerceIn(0, playbackVolumeMaxLevel)
            }
        }
    }

    private fun resetPlaybackBrightness() {
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private fun isUsingSystemBrightness(level: Int = playbackBrightnessLevel): Boolean {
        return level == DEFAULT_GESTURE_LEVEL &&
            window.attributes.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    private fun registerBrightnessObserverIfNeeded() {
        if (brightnessObserverRegistered) return

        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            brightnessObserver,
        )
        brightnessObserverRegistered = true
    }

    private fun unregisterBrightnessObserver() {
        if (!brightnessObserverRegistered) return

        contentResolver.unregisterContentObserver(brightnessObserver)
        brightnessObserverRegistered = false
    }

    private fun registerPictureInPictureActionReceiverIfNeeded() {
        if (pictureInPictureActionReceiverRegistered) return

        ContextCompat.registerReceiver(
            this,
            pictureInPictureActionReceiver,
            IntentFilter(ACTION_PICTURE_IN_PICTURE_TOGGLE_PLAYBACK),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        pictureInPictureActionReceiverRegistered = true
    }

    private fun unregisterPictureInPictureActionReceiver() {
        if (!pictureInPictureActionReceiverRegistered) return

        unregisterReceiver(pictureInPictureActionReceiver)
        pictureInPictureActionReceiverRegistered = false
    }

    private fun togglePlaybackFromPictureInPicture() {
        val currentPlayer = player ?: return

        if (latestPlaybackSnapshot.playbackEnded) {
            currentPlayer.seekTo(0L)
        }

        if (currentPlayer.isPlaying) {
            currentPlayer.pause()
        } else {
            currentPlayer.play()
        }

        latestPlaybackSnapshot = currentPlayer.capturePlaybackSnapshot()
        updatePictureInPictureParams(latestPlaybackSnapshot)
    }

    private fun enterPictureInPictureFromControls() {
        enterPictureInPictureIfAvailable()
    }

    private fun enterPictureInPictureIfAvailable(): Boolean {
        if (!canUsePictureInPicture()) return false

        val params = buildPictureInPictureParams(latestPlaybackSnapshot)
        pendingPictureInPictureOnPause = true
        setPictureInPictureParams(params)
        return enterPictureInPictureMode(params).also { entered ->
            if (!entered) {
                pendingPictureInPictureOnPause = false
            }
        }
    }

    private fun canAutoEnterPictureInPicture(): Boolean {
        return canUsePictureInPicture() &&
            latestPlaybackSnapshot.isPlaying &&
            !latestPlaybackSnapshot.isLoading
    }

    private fun canUsePictureInPicture(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            supportsPictureInPicture &&
            pictureInPictureEnabled &&
            !isInPictureInPictureModeState
    }

    private fun updatePictureInPictureParams(snapshot: VideoPlayerPlaybackSnapshot) {
        if (!supportsPictureInPicture || !pictureInPictureEnabled) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        setPictureInPictureParams(buildPictureInPictureParams(snapshot))
    }

    private fun buildPictureInPictureParams(snapshot: VideoPlayerPlaybackSnapshot): PictureInPictureParams {
        val paramsBuilder = PictureInPictureParams.Builder()
            .setAspectRatio(resolvePictureInPictureAspectRatio(snapshot))
            .setActions(buildPictureInPictureActions(snapshot))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paramsBuilder.setAutoEnterEnabled(snapshot.isPlaying && !snapshot.isLoading)
        }

        return paramsBuilder.build()
    }

    private fun resolvePictureInPictureAspectRatio(snapshot: VideoPlayerPlaybackSnapshot): Rational {
        val videoSize = player?.videoSize
        val width = videoSize?.width ?: 0
        val height = videoSize?.height ?: 0

        return if (width > 0 && height > 0) {
            Rational(width, height)
        } else if (snapshot.durationMs > 0L) {
            DEFAULT_PICTURE_IN_PICTURE_ASPECT_RATIO
        } else {
            DEFAULT_PICTURE_IN_PICTURE_ASPECT_RATIO
        }
    }

    private fun buildPictureInPictureActions(snapshot: VideoPlayerPlaybackSnapshot): List<RemoteAction> {
        val isPlaying = snapshot.isPlaying && !snapshot.playbackEnded
        val title = if (isPlaying) {
            stringResource(MR.strings.action_pause)
        } else {
            stringResource(MR.strings.action_play)
        }
        val iconRes = if (isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_PICTURE_IN_PICTURE_TOGGLE_PLAYBACK).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return listOf(
            RemoteAction(
                Icon.createWithResource(this, iconRes),
                title,
                title,
                pendingIntent,
            ),
        )
    }

    companion object {
        private const val ACTION_PICTURE_IN_PICTURE_TOGGLE_PLAYBACK =
            "eu.kanade.tachiyomi.ui.video.player.action.TOGGLE_PLAYBACK"
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_OWNER_VIDEO_ID = "owner_video_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_BYPASS_MERGE = "bypass_merge"
        private const val INVALID_ID = -1L
        private const val PROGRESS_SAVE_INTERVAL_MS = 10_000L
        private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3_000L
        private const val PLAYBACK_SNAPSHOT_INTERVAL_MS = 250L
        private const val PAUSED_PLAYBACK_SNAPSHOT_INTERVAL_MS = 750L
        private const val SEEK_INCREMENT_MS = 5_000L
        private const val SEEK_FEEDBACK_BURST_WINDOW_MS = 900L
        private val DEFAULT_PICTURE_IN_PICTURE_ASPECT_RATIO = Rational(16, 9)

        fun newIntent(
            context: Context,
            entry: tachiyomi.domain.entry.model.Entry,
            chapter: tachiyomi.domain.entry.model.EntryChapter,
            ownerEntryId: Long = chapter.entryId,
            bypassMerge: Boolean = false,
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_VIDEO_ID, entry.id)
                putExtra(EXTRA_OWNER_VIDEO_ID, ownerEntryId)
                putExtra(EXTRA_EPISODE_ID, chapter.id)
                putExtra(EXTRA_BYPASS_MERGE, bypassMerge)
            }
        }
    }
}

private data class SideGestureState(
    val startX: Float,
    val startY: Float,
    val type: VideoPlayerSideGestureType? = null,
    val initialLevel: Int,
    val active: Boolean = false,
)

private data class HoldSpeedGestureState(
    val startX: Float,
    val startY: Float,
    val longPressTriggered: Boolean = false,
    val cancelled: Boolean = false,
)

private fun brightnessOverlayLevelFor(level: Int): Int {
    return if (level <= 0) -75 else 0
}

internal data class PlayerTouchZoneBounds(
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
) {
    fun contains(xFraction: Float, yFraction: Float): Boolean {
        return xFraction in leftFraction..rightFraction && yFraction in topFraction..bottomFraction
    }
}

internal fun holdSpeedZoneBounds(): List<PlayerTouchZoneBounds> {
    return listOf(
        PlayerTouchZoneBounds(
            leftFraction = HOLD_SPEED_TOP_ZONE_LEFT_FRACTION,
            topFraction = HOLD_SPEED_TOP_ZONE_TOP_FRACTION,
            rightFraction = HOLD_SPEED_TOP_ZONE_RIGHT_FRACTION,
            bottomFraction = HOLD_SPEED_TOP_ZONE_BOTTOM_FRACTION,
        ),
        PlayerTouchZoneBounds(
            leftFraction = HOLD_SPEED_ZONE_START_FRACTION,
            topFraction = HOLD_SPEED_ZONE_TOP_FRACTION,
            rightFraction = HOLD_SPEED_ZONE_END_FRACTION,
            bottomFraction = HOLD_SPEED_ZONE_BOTTOM_FRACTION,
        ),
    )
}

private fun isTouchInHoldSpeedZone(
    touchX: Float,
    touchY: Float,
    playerWidth: Int,
    playerHeight: Int,
): Boolean {
    if (playerWidth <= 0 || playerHeight <= 0) {
        return false
    }

    val width = playerWidth.toFloat()
    val height = playerHeight.toFloat()
    val xFraction = (touchX / width).coerceIn(0f, 1f)
    val yFraction = (touchY / height).coerceIn(0f, 1f)
    return holdSpeedZoneBounds().any { zone ->
        zone.contains(xFraction = xFraction, yFraction = yFraction)
    }
}

private fun resolveSideGestureType(
    startX: Float,
    startY: Float,
    playerWidth: Int,
    playerHeight: Int,
): VideoPlayerSideGestureType? {
    if (playerWidth <= 0 || playerHeight <= 0) {
        return null
    }

    val width = playerWidth.toFloat()
    val height = playerHeight.toFloat()
    val zoneTop = (height * (1f - SIDE_GESTURE_ZONE_HEIGHT_FRACTION)) / 2f
    val zoneBottom = height - zoneTop
    if (startY !in zoneTop..zoneBottom) {
        return null
    }

    val zoneWidth = width * SIDE_GESTURE_ZONE_WIDTH_FRACTION
    val leftZoneStart = width * SIDE_GESTURE_ZONE_HORIZONTAL_INSET_FRACTION
    if (startX in leftZoneStart..(leftZoneStart + zoneWidth)) {
        return VideoPlayerSideGestureType.Brightness
    }

    val rightZoneEnd = width * (1f - SIDE_GESTURE_ZONE_HORIZONTAL_INSET_FRACTION)
    if (startX in (rightZoneEnd - zoneWidth)..rightZoneEnd) {
        return VideoPlayerSideGestureType.Volume
    }

    return null
}

internal fun verticalGestureFraction(
    startY: Float,
    currentY: Float,
    playerWidth: Int,
    playerHeight: Int,
): Float {
    if (playerWidth <= 0 || playerHeight <= 0) {
        return 0f
    }

    val gestureDistance = minOf(playerWidth, playerHeight).toFloat()
    return ((startY - currentY) / gestureDistance).coerceIn(-1f, 1f)
}

internal fun VideoSize.displayAspectRatioOrNull(): Float? {
    if (width <= 0 || height <= 0 || !pixelWidthHeightRatio.isFinite() || pixelWidthHeightRatio <= 0f) {
        return null
    }

    return (width * pixelWidthHeightRatio / height)
        .takeIf { it.isFinite() && it > 0f }
}

private fun screenBrightnessForGestureLevel(level: Int): Float {
    val normalizedLevel = level.coerceIn(MIN_GESTURE_LEVEL, MAX_GESTURE_LEVEL)
    if (normalizedLevel <= MIN_GESTURE_LEVEL) {
        return MIN_POSITIVE_BRIGHTNESS
    }

    val gestureFraction = normalizedLevel / MAX_GESTURE_LEVEL.toFloat()
    return (
        MIN_POSITIVE_BRIGHTNESS +
            (1f - MIN_POSITIVE_BRIGHTNESS) * gestureFraction.pow(BRIGHTNESS_RESPONSE_GAMMA)
        ).coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
}

private fun gestureLevelForScreenBrightness(screenBrightness: Float): Int {
    val clampedBrightness = screenBrightness.coerceIn(MIN_POSITIVE_BRIGHTNESS, 1f)
    val brightnessFraction = (
        (clampedBrightness - MIN_POSITIVE_BRIGHTNESS) /
            (1f - MIN_POSITIVE_BRIGHTNESS)
        ).coerceIn(0f, 1f)
    return (brightnessFraction.pow(1f / BRIGHTNESS_RESPONSE_GAMMA) * MAX_GESTURE_LEVEL)
        .roundToInt()
        .coerceIn(MIN_GESTURE_LEVEL, MAX_GESTURE_LEVEL)
}

private fun AudioManager.safeMusicStreamMaxVolume(): Int =
    getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

@Suppress("DEPRECATION")
private fun Player.canSetDeviceVolumeCompat(): Boolean =
    isCommandAvailable(Player.COMMAND_SET_DEVICE_VOLUME)

@Suppress("DEPRECATION")
private fun Player.setDeviceVolumeCompat(volume: Int) {
    setDeviceVolume(volume)
}

@OptIn(markerClass = [UnstableApi::class])
private class EpisodeNavigationPlayer(
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

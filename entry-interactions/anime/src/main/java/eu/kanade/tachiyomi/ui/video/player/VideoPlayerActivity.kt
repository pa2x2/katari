package eu.kanade.tachiyomi.ui.video.player

import android.app.Activity
import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import eu.kanade.tachiyomi.ui.video.player.background.VideoPlayerBackgroundPlayback
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import eu.kanade.tachiyomi.ui.video.player.pip.VideoPlayerPictureInPicture
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackController
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerScreen
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerScreenHost
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerSettingBindings
import eu.kanade.tachiyomi.ui.video.player.screen.VideoPlayerVisitState
import eu.kanade.tachiyomi.ui.video.player.volume.VideoPlayerVolume
import eu.kanade.tachiyomi.util.system.toast
import mihon.core.metro.metroGraph
import mihon.entry.interactions.anime.R
import mihon.entry.interactions.runtime.EntryInteractionActivityGraph
import mihon.entry.interactions.settings.AnimePlayerPreferences
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.launchEntryChildWebViewAction
import mihon.entry.viewer.settings.navigation.openViewerSettings
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/**
 * The anime player. It holds the window setup, the lifecycle and the owners that outlive each episode's composition:
 * the watched player's playback, picture-in-picture, background playback, brightness and volume. The screen itself is
 * [VideoPlayerScreen].
 */
class VideoPlayerActivity : AppCompatActivity() {

    private val viewModel by viewModels<VideoPlayerViewModel> { metroGraph<ViewModelGraph>().metroViewModelFactory }
    private val dependencies by lazy { metroGraph<AnimePlayerGraph>().animePlayerDependencies }
    private val settings by lazy {
        VideoPlayerSettingBindings(dependencies.viewerSettingBinder, dependencies.animePlayerPreferences)
    }
    private val windowInsetsController by lazy { WindowInsetsControllerCompat(window, window.decorView) }
    private val visit by lazy {
        VideoPlayerVisitState(
            gestureGuideVisible = !dependencies.animePlayerPreferences.gestureGuideShown.get(),
            showsRemainingTime = dependencies.animePlayerPreferences.showsRemainingTime.get(),
        )
    }
    private val playback by lazy {
        VideoPlayerPlaybackController(lifecycleScope) { positionMs, durationMs, activeDurationMs ->
            viewModel.persistPlayback(
                positionMs = positionMs,
                durationMs = durationMs,
                activeDurationMs = activeDurationMs,
            )
        }
    }
    private val pictureInPicture by lazy { VideoPlayerPictureInPicture(this) { playback.player } }
    private val background by lazy { VideoPlayerBackgroundPlayback(this) }
    private val brightness by lazy { VideoPlayerBrightness(window, contentResolver) }
    private val volume by lazy {
        VideoPlayerVolume(getSystemService(Context.AUDIO_SERVICE) as AudioManager) { playback.player }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val activityGraph = metroGraph<EntryInteractionActivityGraph>()
        registerAnimePlayerSecureScreen(activityGraph.readerBasePreferences, activityGraph.securityPreferences)
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
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hideSystemUi()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)

        pictureInPicture.onCreate(autoEnter = settings.autoEnterPictureInPicture.resolveProfile().effectiveValue)
        background.setEnabled(settings.backgroundPlayback.resolveProfile().effectiveValue)
        brightness.sync()
        volume.sync()
        brightness.registerObserver()

        val animeId = intent.extras?.getLong(EXTRA_VIDEO_ID, INVALID_ID) ?: INVALID_ID
        val ownerAnimeId = intent.extras?.getLong(EXTRA_OWNER_VIDEO_ID, animeId) ?: animeId
        val episodeId = intent.extras?.getLong(EXTRA_EPISODE_ID, INVALID_ID) ?: INVALID_ID
        val bypassMerge = intent.extras?.getBoolean(EXTRA_BYPASS_MERGE, false) ?: false
        if (animeId == INVALID_ID || episodeId == INVALID_ID) {
            finish()
            return
        }
        viewModel.init(
            entryId = animeId,
            chapterId = episodeId,
            ownerEntryId = ownerAnimeId,
            bypassMerge = bypassMerge,
            defaultPlaybackSpeed = settings.defaultPlaybackSpeedPercent.resolveProfile().effectiveValue / 100f,
        )

        val host = VideoPlayerScreenHost(
            engine = dependencies.videoEngine,
            mpvEngine = dependencies.mpvEngine,
            mpvAddon = dependencies.mpvAddon,
            mpvAddonInstallation = dependencies.mpvAddonInstallation,
            preferences = dependencies.animePlayerPreferences,
            settings = settings,
            visit = visit,
            playback = playback,
            pictureInPicture = pictureInPicture,
            brightness = brightness,
            volume = volume,
            background = background,
            onClose = ::finish,
            onHideSystemUi = ::hideSystemUi,
            onOpenAddonSettings = {
                openViewerSettings(AnimePlayerPreferences.PROVIDER_ID, highlight = stringResource(MR.strings.mpv_addon))
            },
            onOpenWebView = ::launchEpisodeWebViewAction,
        )
        setPlayerComposeContent {
            VideoPlayerScreen(
                activity = this,
                viewModel = viewModel,
                host = host,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pausedStartPositionMs(intent)?.let { visit.pendingPausedStartMs = it }
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
        background.setEnabled(settings.backgroundPlayback.resolveProfile().effectiveValue)
        pictureInPicture.onResume(autoEnter = settings.autoEnterPictureInPicture.resolveProfile().effectiveValue)
        brightness.sync()
        volume.sync(playback.player)
        hideSystemUi()
    }

    override fun onPause() {
        playback.flush()
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
            volume.sync(playback.player)
        }
        return handled
    }

    override fun onUserLeaveHint() {
        pictureInPicture.onUserLeaveHint()
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPicture.onModeChanged(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            // Closing the picture-in-picture window stops the activity while it is still in picture-in-picture, so
            // onStop let the video play on. The mode ends afterwards, with the activity already stopped.
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                pauseUnlessPlayingInBackground()
                playback.flush()
            }
            hideSystemUi()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        val resolution = (viewModel.state.value as? VideoPlayerViewModel.State.Ready)?.childWebView
        resolution?.url?.let { outContent.webUri = android.net.Uri.parse(it) }
    }

    override fun onStop() {
        pauseUnlessPlayingInBackground()
        playback.flush()
        super.onStop()
    }

    override fun onDestroy() {
        brightness.unregisterObserver()
        pictureInPicture.release()
        brightness.reset()
        background.release()
        playback.release(persistState = false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun pauseUnlessPlayingInBackground() {
        if (pictureInPicture.pausesOnStop && !background.enabled) {
            playback.player?.pause()
        }
    }

    private fun hideSystemUi() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun launchEpisodeWebViewAction(
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
        title: String,
    ) {
        pictureInPicture.suppressForNavigation()
        launchEntryChildWebViewAction(action, resolution, title)
            .onFailure {
                pictureInPicture.onNavigationFailed()
                toast(it.message)
            }
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_OWNER_VIDEO_ID = "owner_video_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_BYPASS_MERGE = "bypass_merge"
        private const val INVALID_ID = -1L

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

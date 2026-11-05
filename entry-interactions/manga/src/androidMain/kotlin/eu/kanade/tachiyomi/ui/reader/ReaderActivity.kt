package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.platform.MaterialContainerTransform
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.ReaderChapterNavigationSheet
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.appbars.ReaderMenuBounds
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderScreen
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderScreenHost
import eu.kanade.tachiyomi.ui.reader.setting.toViewer
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupErrorScreen
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupLoadingScreen
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupState
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.key.readerKeyCommand
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.core.metro.metroGraph
import mihon.entry.interactions.manga.R
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.manga.reader.text.interaction.MangaReaderTextInteraction
import mihon.entry.interactions.manga.reader.text.session.MangaTextProcessAheadPolicy
import mihon.entry.interactions.manga.reader.text.ui.MangaReaderTextFeatures
import mihon.entry.interactions.reader.image.toReaderShareIntent
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.interactions.reader.settings.activityOrientation
import mihon.entry.interactions.runtime.EntryInteractionActivity
import mihon.entry.interactions.runtime.EntryInteractionActivityGraph
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.launchEntryChildWebViewAction
import mihon.entry.viewer.chrome.ui.position.ReaderPageIndicator
import mihon.entry.viewer.chrome.ui.position.ReaderUnderPages
import mihon.entry.viewer.settings.navigation.openViewerSettings
import mihon.presentation.kit.dialog.ProgressDialog
import mihon.text.recognition.api.host.openTextRecognitionPipelineChoice
import mihon.translation.api.host.openTranslationSettings
import mihon.tts.ui.playback.ShortFormSpeechFailure
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

class ReaderActivity : EntryInteractionActivity() {

    companion object {
        private const val EXTRA_MANGA = "manga"
        private const val EXTRA_CHAPTER = "chapter"
        private const val EXTRA_PAGE = "page"

        fun newIntent(context: Context, entry: Entry, chapter: EntryChapter, pageIndex: Int? = null): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_MANGA, entry.id)
                putExtra(EXTRA_CHAPTER, chapter.id)
                pageIndex?.let { putExtra(EXTRA_PAGE, it) }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private val preferences by lazy { metroGraph<EntryInteractionActivityGraph>().readerBasePreferences }

    internal val uiPreferences by lazy { metroGraph<MangaReaderGraph>().uiPreferences }

    internal lateinit var views: ReaderActivityViews

    internal val viewModel by viewModels<ReaderViewModel> { metroGraph<ViewModelGraph>().metroViewModelFactory }

    /** Settings for the opened entry; available once the entry is loaded. */
    internal val readerSettings: MangaReaderSettingsBindings
        get() = viewModel.readerSettings

    /**
     * Configuration at reader level, like background color or forced orientation.
     */
    private var config: ReaderConfig? = null

    private lateinit var systemBars: ReaderSystemBars
    private var readerBackgroundColor: Int? = null

    private var loadingIndicator: ReaderProgressIndicator? = null

    internal lateinit var controller: MangaReaderController
        private set

    // The View viewer showing the pages (pager, webtoon, ...), which this activity makes and the controller owns
    private val viewer: Viewer? get() = if (::controller.isInitialized) controller.viewer.value as Viewer? else null
    private var appliedRuntimeSettings: ReaderRuntimeSettings? = null

    val isNavigatingFromControls: Boolean get() = controller.isNavigatingFromControls

    /**
     * Called when the activity is created. Initializes the presenter and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val activityGraph = metroGraph<EntryInteractionActivityGraph>()
        registerReaderSecureScreen(activityGraph.readerBasePreferences, activityGraph.securityPreferences)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }

        enableEdgeToEdge()
        systemBars = ReaderSystemBars(window, lifecycle)

        super.onCreate(savedInstanceState)

        views = ReaderActivityViews.inflate(layoutInflater)
        setContentView(views.root)

        if (!viewModel.hasValidArgs) {
            finish()
            return
        }

        controller = MangaReaderController(
            viewModel = viewModel,
            scope = lifecycleScope,
            overlayOrigin = {
                val origin = IntArray(2).also(views.composeOverlay::getLocationInWindow)
                origin[0].toFloat() to origin[1].toFloat()
            },
            onChaptersSet = { views.readerContainer.removeView(loadingIndicator) },
        )
        val host = screenHost(activityGraph)
        views.composeOverlay.setReaderComposeContent { MangaReaderScreen(controller, host) }

        dismissNewChaptersNotification(this, viewModel.mangaId.hashCode())

        viewModel.state
            .map { it.menuVisible }
            .distinctUntilChanged()
            .onEach { applySystemBars() }
            .launchIn(lifecycleScope)
        viewModel.settingsBindings
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { settings -> config = ReaderConfig(settings) }
            .launchIn(lifecycleScope)

        // Finish when incognito mode is disabled
        preferences.incognitoMode.changes()
            .drop(1)
            .onEach { if (!it) finish() }
            .launchIn(lifecycleScope)

        viewModel.state
            .map { state ->
                state.manga?.let { manga ->
                    ReaderRuntimeSettings(
                        entryId = manga.id,
                        readingMode = state.effectiveReadingMode,
                        orientation = state.effectiveOrientation,
                    )
                }
            }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::applyRuntimeSettings)
            .launchIn(lifecycleScope)
    }

    private fun screenHost(graph: EntryInteractionActivityGraph) = MangaReaderScreenHost(
        context = this,
        sharedSettingsRegistry = graph.readerSharedSettingsRegistry,
        webViewActions = EntryChildWebViewAction.entries,
        hasDisplayCutout = { hasDisplayCutout() },
        onNavigateUp = onBackPressedDispatcher::onBackPressed,
        onClose = ::finish,
        onOpenEntry = ::openMangaScreen,
        onOpenWebView = ::launchChapterWebViewAction,
        onOpenDefaultSettings = { openViewerSettings(MangaReaderSettings.PROVIDER_ID) },
        textLayer = { menuVisible, menuBounds ->
            MangaReaderTextFeatures(
                controller = controller,
                menuVisible = menuVisible,
                menuBounds = menuBounds,
                onChoosePipeline = ::openTextRecognitionPipelineChoice,
                onOpenTranslationSettings = ::openTranslationSettings,
            )
        },
    )

    /**
     * Called when the activity is destroyed. Cleans up the viewer, configuration and any view.
     */
    override fun onDestroy() {
        systemBars.release()
        super.onDestroy()
        if (::controller.isInitialized) controller.destroy()
        config = null
    }

    override fun onStop() {
        controller.onStopped()
        super.onStop()
    }

    override fun onPause() {
        controller.onPaused()
        super.onPause()
    }

    // The system bars are applied again on resume and on focus, as rotations and other windows can show them
    override fun onResume() {
        super.onResume()
        controller.onResumed()
        applySystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applySystemBars()
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        controller.chapterWebView?.url?.let { outContent.webUri = it.toUri() }
    }

    // The reader slides out the way it slid in
    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    // Ahead of the views, so the arrows never move focus instead
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = handleAutoScrollKeyEvent(event) ||
            (viewer?.handleKeyEvent(event) ?: false) ||
            handleReaderKeyEvent(event)
        return handled || super.dispatchKeyEvent(event)
    }

    // The reader's shared keys run as the key comes back up, as Android's buttons do
    private fun handleReaderKeyEvent(event: KeyEvent): Boolean {
        if (viewer == null) return false
        val command = readerKeyCommand(ComposeKeyEvent(event)) ?: return false
        if (event.action == KeyEvent.ACTION_UP) controller.onKeyCommand(command)
        return true
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    private fun applySystemBars() {
        val settings = viewModel.settingsBindings.value
        systemBars.apply(
            menuVisible = viewModel.state.value.menuVisible,
            keepStatusBarVisible = settings?.showStatusBar?.state?.value?.effectiveValue == true,
            keepNavigationBarVisible = settings?.showNavigationBar?.state?.value?.effectiveValue == true,
            readerBackgroundColor = readerBackgroundColor,
        )
    }

    /**
     * Called from the presenter when a manga is ready. Used to instantiate the appropriate viewer.
     */
    private fun applyRuntimeSettings(settings: ReaderRuntimeSettings) {
        val previousSettings = appliedRuntimeSettings
        when (settings.changeFrom(previousSettings)) {
            ReaderRuntimeSettings.Change.RecreateViewer -> updateViewer()
            ReaderRuntimeSettings.Change.UpdateOrientation -> setOrientation(settings.orientation)
            ReaderRuntimeSettings.Change.None -> Unit
        }
        appliedRuntimeSettings = settings
    }

    private fun updateViewer() {
        val prevViewer = viewer
        val newViewer = ReadingMode.toViewer(viewModel.getMangaReadingMode(), this)

        if (window.sharedElementEnterTransition is MaterialContainerTransform) {
            // Wait until transition is complete to avoid crash on API 26
            window.sharedElementEnterTransition.doOnEnd {
                setOrientation(viewModel.getMangaOrientation())
            }
        } else {
            setOrientation(viewModel.getMangaOrientation())
        }

        controller.setViewer(newViewer)
        if (prevViewer != null) views.viewerContainer.removeAllViews()
        updateViewerInset(viewModel.readerSettings.drawUnderCutout.state.value.effectiveValue)
        views.viewerContainer.addView(newViewer.getView())

        loadingIndicator = ReaderProgressIndicator(this)
        views.readerContainer.addView(loadingIndicator)

        startPostponedEnterTransition()
    }

    private fun openMangaScreen() {
        viewModel.manga?.id?.let { id ->
            startActivity(mangaEntryIntent(id))
        }
    }

    private fun launchChapterWebViewAction(
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
    ) {
        launchEntryChildWebViewAction(action, resolution, viewModel.manga?.displayTitle)
            .onFailure { toast(it.message) }
    }

    // What the viewers report and ask for, passed on to the controller

    internal fun onPageSelected(page: ReaderPage, facing: ReaderPage? = null) = controller.onPageSelected(page, facing)

    internal fun onSpreadPageMiddles(pageNumber: Int, middles: Pair<Float, Float>) =
        controller.onSpreadPageMiddles(pageNumber, middles)

    internal fun onPageTextSurfacesChanged() = controller.onPageTextSurfacesChanged()

    /** Called from viewers before a tap is used for navigation. Returns whether translate mode consumed the tap. */
    internal fun onTextTap(event: MotionEvent): Boolean {
        val windowOrigin = IntArray(2).also(window.decorView::getLocationOnScreen)
        return controller.textInteraction.onTap(event.rawX - windowOrigin[0], event.rawY - windowOrigin[1])
    }

    internal fun onPageLongTap(page: ReaderPage) = controller.onPageLongTap(page)

    internal fun requestTransitionChapterLoad(chapter: ReaderChapter) = controller.requestTransitionChapterLoad(chapter)

    internal fun onViewerDirectManipulation() = controller.onViewerDirectManipulation()

    fun toggleMenu() = controller.toggleMenu()

    fun showMenu() = controller.setMenuVisibility(true)

    fun hideMenu() = controller.setMenuVisibility(false)

    // While auto scroll runs, the volume keys change its speed instead of turning pages
    private fun handleAutoScrollKeyEvent(event: KeyEvent): Boolean {
        if (!controller.isAutoScrollRunning || !controller.supportsAutoScroll()) return false
        val delta = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> 1
            KeyEvent.KEYCODE_VOLUME_UP -> -1
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_UP) controller.changeAutoScrollSpeed(delta)
        return true
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    private fun setOrientation(orientation: Int) {
        val newOrientation = ReaderOrientation.fromPreference(orientation)
        if (newOrientation.activityOrientation != requestedOrientation) {
            requestedOrientation = newOrientation.activityOrientation
        }
    }

    /**
     * Keeps the viewer edge-to-edge behind system bars so showing or hiding them never resizes the page, and only
     * insets it from the display cutout when the reader must not draw there.
     */
    private fun updateViewerInset(drawUnderCutout: Boolean) {
        if (!::views.isInitialized) return
        val view = views.viewerContainer

        view.applyInsetsPadding(ViewCompat.getRootWindowInsets(view), drawUnderCutout)
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, windowInsets ->
            view.applyInsetsPadding(windowInsets, drawUnderCutout)
            windowInsets
        }
    }

    private fun View.applyInsetsPadding(
        windowInsets: WindowInsetsCompat?,
        drawUnderCutout: Boolean,
    ) {
        val insets = windowInsets
            ?.takeUnless { drawUnderCutout }
            ?.getInsets(WindowInsetsCompat.Type.displayCutout())
            ?: Insets.NONE

        setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    /**
     * Class that handles the user preferences of the reader.
     */
    private inner class ReaderConfig(
        private val settings: MangaReaderSettingsBindings,
    ) {

        /*
         * Initializes the reader subscriptions.
         */
        init {
            settings.readerTheme.state
                .map { it.effectiveValue }
                .distinctUntilChanged()
                .onEach { theme ->
                    val backgroundColor = baseContext.readerPageBackgroundColor(theme)
                    views.readerContainer.setBackgroundColor(backgroundColor)
                    readerBackgroundColor = backgroundColor
                    applySystemBars()
                }
                .launchIn(lifecycleScope)

            preferences.displayProfile.changes()
                .onEach { setDisplayProfile(it) }
                .launchIn(lifecycleScope)

            settings.keepScreenOn.state
                .map { it.effectiveValue }
                .distinctUntilChanged()
                .onEach(::setKeepScreenOn)
                .launchIn(lifecycleScope)

            settings.customBrightness.state
                .map { it.effectiveValue }
                .distinctUntilChanged()
                .onEach(::setCustomBrightness)
                .launchIn(lifecycleScope)

            settings.drawUnderCutout.state
                .map { it.effectiveValue }
                .distinctUntilChanged()
                .onEach(::updateViewerInset)
                .launchIn(lifecycleScope)

            combine(
                settings.showStatusBar.state.map { it.effectiveValue }.distinctUntilChanged(),
                settings.showNavigationBar.state.map { it.effectiveValue }.distinctUntilChanged(),
            ) { _, _ -> }
                .onEach { applySystemBars() }
                .launchIn(lifecycleScope)
        }

        /**
         * Sets the display profile to [path].
         */
        private fun setDisplayProfile(path: String) {
            val file = metroGraph<MangaReaderGraph>().storageFiles.fromUri(path)
            if (file != null && file.exists()) {
                val inputStream = file.openInputStream()
                val outputStream = ByteArrayOutputStream()
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val data = outputStream.toByteArray()
                TachiyomiImageDecoder.displayProfile = data
            }
        }

        /**
         * Sets the keep screen on mode according to [enabled].
         */
        private fun setKeepScreenOn(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        /**
         * Sets the custom brightness overlay according to [enabled].
         */
        private fun setCustomBrightness(enabled: Boolean) {
            if (enabled) {
                settings.customBrightnessValue.state
                    .map { it.effectiveValue }
                    .distinctUntilChanged()
                    .sample(0.1.seconds)
                    .onEach(::setCustomBrightnessValue)
                    .launchIn(lifecycleScope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        /**
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set reader brightness.
            val readerBrightness = when {
                value > 0 -> {
                    value / 100f
                }
                value < 0 -> {
                    0.01f
                }
                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

            viewModel.setBrightnessOverlayValue(value)
        }
    }
}

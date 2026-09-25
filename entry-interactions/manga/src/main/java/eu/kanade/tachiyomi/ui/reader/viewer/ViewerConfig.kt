package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.viewer.settings.ViewerSettingBinding

/**
 * Common configuration for all viewers.
 *
 * Values are read from the opened entry's settings bindings so a per-series override is reflected by the live viewer.
 */
internal abstract class ViewerConfig(
    protected val settings: MangaReaderSettingsBindings,
    private val scope: CoroutineScope,
) {

    var imagePropertyChangedListener: (() -> Unit)? = null

    var navigationModeChangedListener: (() -> Unit)? = null

    var tappingInverted = MangaReaderSettings.TappingInvertMode.NONE
    var longTapEnabled = true
    var usePageTransitions = false
    var doubleTapAnimDuration = 500
    var volumeKeysEnabled = false
    var volumeKeysInverted = false

    /**
     * Snapshot state so transition views already on screen follow mode changes without rebinding.
     */
    var chapterTransitionMode: ChapterTransitionMode by mutableStateOf(ChapterTransitionMode.ALWAYS)
    var navigationMode = 0
        protected set

    var forceNavigationOverlay = false

    var navigationOverlayOnStart = false

    var dualPageSplit = false
        protected set

    var dualPageInvert = false
        protected set

    var dualPageRotateToFit = false
        protected set

    var dualPageRotateToFitInvert = false
        protected set

    abstract var navigator: ViewerNavigation
        protected set

    init {
        settings.readWithLongTap.register({ longTapEnabled = it })

        settings.pageTransitions.register({ usePageTransitions = it })

        settings.doubleTapAnimSpeed.register({ doubleTapAnimDuration = it })

        settings.volumeKeys.register({ volumeKeysEnabled = it })

        settings.volumeKeysInverted.register({ volumeKeysInverted = it })

        settings.chapterTransition.register({ chapterTransitionMode = it })

        forceNavigationOverlay = settings.showNavigationOverlayNewUser.state.value.effectiveValue
        if (forceNavigationOverlay) {
            settings.showNavigationOverlayNewUser.setProfileValue(false)
        }

        settings.showNavigationOverlayOnStart.register({ navigationOverlayOnStart = it })
    }

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)

    protected fun <T> ViewerSettingBinding<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {},
    ) {
        state
            .map { it.effectiveValue }
            .distinctUntilChanged()
            .onEach { value ->
                valueAssignment(value)
                onChanged(value)
            }
            .launchIn(scope)
    }
}

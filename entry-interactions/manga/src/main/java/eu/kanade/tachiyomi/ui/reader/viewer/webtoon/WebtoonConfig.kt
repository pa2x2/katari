package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings

/**
 * Configuration used by webtoon viewers.
 */
internal class WebtoonConfig(
    settings: MangaReaderSettingsBindings,
    scope: CoroutineScope,
) : ViewerConfig(settings, scope) {

    var themeChangedListener: (() -> Unit)? = null

    var imageCropBorders = false
        private set

    var zoomOutDisabled = false
        private set

    var zoomPropertyChangedListener: ((Boolean) -> Unit)? = null

    var sidePadding = 0
        private set

    var doubleTapZoom = true
        private set

    var doubleTapZoomChangedListener: ((Boolean) -> Unit)? = null

    var scrollHideThreshold = settings.readerHideThreshold.state.value.effectiveValue.threshold
        private set

    var theme = settings.readerTheme.state.value.effectiveValue
        private set

    init {
        settings.readerHideThreshold
            .register({ scrollHideThreshold = it.threshold })

        settings.cropBordersWebtoon
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        settings.webtoonSidePadding
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })

        settings.webtoonNavigationMode
            .register({ navigationMode = it }, { updateNavigation(it) })

        settings.webtoonNavigationInverted
            .register({ tappingInverted = it }, { navigator.invertMode = it })
        settings.webtoonNavigationInverted.state
            .map { it.effectiveValue }
            .drop(1)
            .onEach { navigationModeChangedListener?.invoke() }
            .launchIn(scope)

        settings.dualPageSplitWebtoon
            .register({ dualPageSplit = it }, { imagePropertyChangedListener?.invoke() })

        settings.dualPageInvertWebtoon
            .register({ dualPageInvert = it }, { imagePropertyChangedListener?.invoke() })

        settings.dualPageRotateToFitWebtoon
            .register(
                { dualPageRotateToFit = it },
                { imagePropertyChangedListener?.invoke() },
            )

        settings.dualPageRotateToFitInvertWebtoon
            .register(
                { dualPageRotateToFitInvert = it },
                { imagePropertyChangedListener?.invoke() },
            )

        settings.webtoonDisableZoomOut
            .register(
                { zoomOutDisabled = it },
                { zoomPropertyChangedListener?.invoke(it) },
            )

        settings.webtoonDoubleTapZoom
            .register(
                { doubleTapZoom = it },
                { doubleTapZoomChangedListener?.invoke(it) },
            )

        settings.readerTheme.state
            .map { it.effectiveValue }
            .distinctUntilChanged()
            .drop(1)
            .onEach {
                theme = it
                themeChangedListener?.invoke()
            }
            .launchIn(scope)
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation {
        return LNavigation()
    }

    override fun updateNavigation(navigationMode: Int) {
        this.navigator = when (navigationMode) {
            0 -> defaultNavigation()
            1 -> LNavigation()
            2 -> KindlishNavigation()
            3 -> EdgeNavigation()
            4 -> RightAndLeftNavigation()
            5 -> DisabledNavigation()
            else -> defaultNavigation()
        }
        navigationModeChangedListener?.invoke()
    }
}

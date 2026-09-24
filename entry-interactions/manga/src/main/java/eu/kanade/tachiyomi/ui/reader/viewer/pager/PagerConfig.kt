package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings

/**
 * Configuration used by pager viewers.
 */
internal class PagerConfig(
    private val viewer: PagerViewer,
    settings: MangaReaderSettingsBindings,
    scope: CoroutineScope,
) : ViewerConfig(settings, scope) {

    var theme = settings.readerTheme.state.value.effectiveValue
        private set

    var automaticBackground = false
        private set

    var dualPageSplitChangedListener: ((Boolean) -> Unit)? = null

    var imageScaleType = 1
        private set

    var imageZoomType = ReaderPageImageView.ZoomStartPosition.LEFT
        private set

    var imageCropBorders = false
        private set

    var navigateToPan = false
        private set

    var landscapeZoom = false
        private set

    init {
        settings.readerTheme
            .register(
                {
                    theme = it
                    automaticBackground = it == 3
                },
                { imagePropertyChangedListener?.invoke() },
            )

        settings.imageScaleType
            .register({ imageScaleType = it }, { imagePropertyChangedListener?.invoke() })

        settings.zoomStart
            .register({ zoomTypeFromPreference(it) }, { imagePropertyChangedListener?.invoke() })

        settings.cropBorders
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        settings.navigateToPan
            .register({ navigateToPan = it })

        settings.landscapeZoom
            .register({ landscapeZoom = it }, { imagePropertyChangedListener?.invoke() })

        settings.pagerNavigationMode
            .register({ navigationMode = it }, { updateNavigation(navigationMode) })

        settings.pagerNavigationInverted
            .register({ tappingInverted = it }, { navigator.invertMode = it })
        settings.pagerNavigationInverted.state
            .map { it.effectiveValue }
            .drop(1)
            .onEach { navigationModeChangedListener?.invoke() }
            .launchIn(scope)

        settings.dualPageSplitPaged
            .register(
                { dualPageSplit = it },
                {
                    imagePropertyChangedListener?.invoke()
                    dualPageSplitChangedListener?.invoke(it)
                },
            )

        settings.dualPageInvertPaged
            .register({ dualPageInvert = it }, { imagePropertyChangedListener?.invoke() })

        settings.dualPageRotateToFit
            .register(
                { dualPageRotateToFit = it },
                { imagePropertyChangedListener?.invoke() },
            )

        settings.dualPageRotateToFitInvert
            .register(
                { dualPageRotateToFitInvert = it },
                { imagePropertyChangedListener?.invoke() },
            )
    }

    private fun zoomTypeFromPreference(value: Int) {
        imageZoomType = when (value) {
            // Auto
            1 -> when (viewer) {
                is L2RPagerViewer -> ReaderPageImageView.ZoomStartPosition.LEFT
                is R2LPagerViewer -> ReaderPageImageView.ZoomStartPosition.RIGHT
                else -> ReaderPageImageView.ZoomStartPosition.CENTER
            }
            // Left
            2 -> ReaderPageImageView.ZoomStartPosition.LEFT
            // Right
            3 -> ReaderPageImageView.ZoomStartPosition.RIGHT
            // Center
            else -> ReaderPageImageView.ZoomStartPosition.CENTER
        }
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = this.tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation {
        return when (viewer) {
            is VerticalPagerViewer -> LNavigation()
            else -> RightAndLeftNavigation()
        }
    }

    override fun updateNavigation(navigationMode: Int) {
        navigator = when (navigationMode) {
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

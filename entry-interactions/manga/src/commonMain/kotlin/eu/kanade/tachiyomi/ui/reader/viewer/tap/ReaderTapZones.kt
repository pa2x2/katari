package eu.kanade.tachiyomi.ui.reader.viewer.tap

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.reader.viewer.current
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.viewer.chrome.ui.tap.ReaderTapAction
import mihon.entry.viewer.chrome.ui.tap.readerTapRegions
import mihon.presentation.kit.gesture.TapZone
import mihon.presentation.kit.gesture.TapZoneOverlay
import mihon.presentation.kit.gesture.TapZoneRole

/**
 * The tap zones of a viewer, shown over its pages for a moment so a reader learns where to tap: when the viewer opens
 * if the setting asks for it or the reader has never seen them, and whenever the zones or their inversion change.
 * The first touch anywhere hides them again.
 */
@Stable
internal class ReaderTapZones(private val settings: MangaReaderSettingsBindings) {
    var visible by mutableStateOf(false)
        private set

    private var seen = false

    internal fun onZonesChanged() {
        val show = if (seen) {
            true
        } else {
            seen = true
            val newReader = settings.showNavigationOverlayNewUser.current
            if (newReader) settings.showNavigationOverlayNewUser.setProfileValue(false)
            newReader || settings.showNavigationOverlayOnStart.current
        }
        if (show) visible = true
    }

    fun hide() {
        visible = false
    }
}

/** The tap zones of a viewer whose zones are [mode] mirrored by [inversion], told each time either changes. */
@Composable
internal fun rememberReaderTapZones(settings: MangaReaderSettingsBindings, mode: Int, inversion: Int): ReaderTapZones {
    val zones = remember(settings) { ReaderTapZones(settings) }
    LaunchedEffect(zones, mode, inversion) { zones.onZonesChanged() }
    return zones
}

/** Hides [zones] at the first touch, without taking the touch from the pages. */
internal fun Modifier.hidesTapZones(zones: ReaderTapZones): Modifier = pointerInput(zones) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        zones.hide()
    }
}

/** Draws [zones] while they show: the regions of [mode], mirrored by [inversion], in a viewer [vertical] or not. */
@Composable
internal fun ReaderTapZonesOverlay(
    zones: ReaderTapZones,
    mode: Int,
    vertical: Boolean,
    inversion: Int,
    modifier: Modifier = Modifier,
) {
    val regions = remember(mode, vertical, inversion) {
        readerTapRegions(mode, vertical, inversion).map {
            TapZone(it.left, it.top, it.right, it.bottom, it.action.role)
        }
    }
    TapZoneOverlay(regions, zones.visible, modifier)
}

private val ReaderTapAction.role: TapZoneRole
    get() = when (this) {
        ReaderTapAction.MENU -> TapZoneRole.Menu
        ReaderTapAction.PREV -> TapZoneRole.Previous
        ReaderTapAction.NEXT -> TapZoneRole.Next
        ReaderTapAction.LEFT -> TapZoneRole.Left
        ReaderTapAction.RIGHT -> TapZoneRole.Right
    }

package eu.kanade.tachiyomi.ui.video.player.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FitScreen
import mihon.icons.materialsymbols.rounded.Fullscreen
import mihon.icons.materialsymbols.rounded.ZoomIn
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

/** Fit, Fill, or a zoom's scale such as "1.35×". */
@Composable
internal fun PictureFraming.label(): String = when (this) {
    PictureFraming.Fit -> stringResource(MR.strings.anime_playback_picture_fit)
    PictureFraming.Fill -> stringResource(MR.strings.anime_playback_picture_fill)
    is PictureFraming.Zoom -> {
        val format = remember { NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 } }
        stringResource(MR.strings.anime_playback_picture_zoom, format.format(scale))
    }
}

internal val PictureFraming.icon: ImageVector
    get() = when (this) {
        PictureFraming.Fit -> MaterialSymbols.Rounded.FitScreen
        PictureFraming.Fill -> MaterialSymbols.Rounded.Fullscreen
        is PictureFraming.Zoom -> MaterialSymbols.Rounded.ZoomIn
    }

/** The toolbar button's next framing: Fit switches to Fill, and Fill or a zoom goes back to Fit. */
internal fun PictureFraming.toggled(): PictureFraming =
    if (this == PictureFraming.Fit) PictureFraming.Fill else PictureFraming.Fit

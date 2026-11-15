package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.presentation.kit.list.SliderRow
import eu.kanade.tachiyomi.ui.video.player.brightness.ExtraDim
import eu.kanade.tachiyomi.ui.video.player.brightness.VideoPlayerBrightness
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

/** How deep the extra dim goes, on a slider from none to the deepest, shown as a share of the way. */
@Composable
internal fun VideoPlayerExtraDimRow(brightness: VideoPlayerBrightness) {
    val depth = (-brightness.level).coerceIn(0, ExtraDim.LEVELS)
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    SliderRow(
        title = stringResource(MR.strings.anime_extra_dim),
        value = depth,
        valueRange = 0..ExtraDim.LEVELS,
        valueText = percentFormat.format(depth / ExtraDim.LEVELS.toFloat()),
        // Fifty levels would crowd the track with tick marks; each still lands on a whole level
        steps = 0,
        onValueChange = { brightness.adjust(-it) },
    )
}

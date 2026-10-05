package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette

/**
 * A page at the size it's shared at: 360 by 640 dp drawn at three pixels per dp, so the image is always 1080 by 1920
 * whatever the screen. The screen scales it to fit; [modifier] applies to the page itself, as captured.
 *
 * @param footer the line at the foot of every page, so a page shared on its own still says whose and when.
 */
@Composable
internal fun RecapPageFrame(
    palette: RecapPalette,
    footer: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalDensity provides Density(PAGE_DENSITY, fontScale = 1f),
        LocalRecapPalette provides palette,
    ) {
        Column(
            modifier = modifier
                .requiredSize(PAGE_WIDTH, PAGE_HEIGHT)
                .clipToBounds()
                .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundEnd)))
                .padding(start = 28.dp, end = 28.dp, top = 40.dp, bottom = 22.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            RecapText(
                text = footer.uppercase(),
                style = RecapTypography.Tiny,
                color = palette.muted,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private const val PAGE_DENSITY = 3f
private val PAGE_WIDTH = 360.dp
private val PAGE_HEIGHT = 640.dp

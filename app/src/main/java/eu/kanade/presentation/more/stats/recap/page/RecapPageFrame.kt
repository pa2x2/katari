package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapText
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.StoryPalette
import mihon.presentation.kit.theme.StoryTheme
import mihon.presentation.kit.theme.TextRole

/**
 * A page at the size it's shared at: [StoryMetrics.pageWidth] by [StoryMetrics.pageHeight] drawn at three pixels per
 * dp, so the image is always 1080 by 1920 whatever the screen. The screen scales it to fit; [modifier] applies to the
 * page itself, as captured.
 *
 * The page has no background of its own: the story draws [drawRecapBackdrop] across the whole screen, so pages run
 * edge to edge, and draws it into a page it shares.
 *
 * @param footer the line at the foot of every page, so a page shared on its own still says whose and when.
 * @param behind drawn behind the content over the whole page, margins included.
 */
@Composable
internal fun RecapPageFrame(
    palette: StoryPalette,
    footer: String,
    modifier: Modifier = Modifier,
    behind: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    StoryTheme(palette, page = true) {
        CompositionLocalProvider(
            LocalDensity provides Density(PAGE_DENSITY, fontScale = 1f),
            LocalRecapPalette provides palette,
        ) {
            Box(modifier.requiredSize(StoryMetrics.pageWidth, StoryMetrics.pageHeight).clipToBounds()) {
                behind()
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = StoryMetrics.pageSideMargin,
                            end = StoryMetrics.pageSideMargin,
                            top = StoryMetrics.pageTopMargin,
                            bottom = StoryMetrics.pageBottomMargin,
                        ),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
                        content = content,
                    )
                    RecapText(
                        text = footer.uppercase(),
                        role = TextRole.Badge,
                        tone = ContentTone.Secondary,
                        modifier = Modifier.padding(top = KatariTheme.spacing.l),
                    )
                }
            }
        }
    }
}

private const val PAGE_DENSITY = 3f

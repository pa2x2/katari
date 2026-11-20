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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import app.katari.presentation.kit.content.StoryMetrics
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.StoryPalette
import app.katari.presentation.kit.theme.StoryTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapText

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
                    PageContent(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
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

/**
 * A page's content in the room above its footer. Its text's height isn't fixed by the type scale alone: a platform
 * sets a line no shorter than its font, so the desktop gives the large figures' tight lines their font's full height,
 * and fonts differ between systems. Content that comes out taller than the room is scaled down to fit, from its top
 * start, instead of losing its last lines at the foot of the page. Its natural height is its intrinsic height, so a
 * page can't hold a layout that composes from its constraints, such as BoxWithConstraints or a lazy list.
 */
@Composable
private fun PageContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Layout(
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l), content = content)
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val column = measurables.single()
        val room = constraints.maxHeight
        val natural = column.minIntrinsicHeight(constraints.maxWidth)
        if (natural <= room) {
            val placeable = column.measure(Constraints.fixed(constraints.maxWidth, room))
            layout(constraints.maxWidth, room) { placeable.place(0, 0) }
        } else {
            val placeable = column.measure(Constraints.fixed(constraints.maxWidth, natural))
            val scale = room / natural.toFloat()
            layout(constraints.maxWidth, room) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}

private const val PAGE_DENSITY = 3f

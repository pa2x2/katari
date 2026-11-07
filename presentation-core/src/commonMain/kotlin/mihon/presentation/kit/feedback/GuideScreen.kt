package mihon.presentation.kit.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.RocketLaunch
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What one page of a [GuideScreen] is about: the icon, heading and line above its content. */
@Immutable
class GuidePageHeading(val icon: ImageVector, val heading: String, val subtitle: String)

/**
 * A guide of several pages a person goes through in order, such as the first-run setup. A bar at the top says how far
 * along the guide [page] is; each page's [heading] and [content] slide in together; the bottom bar goes on with
 * [nextText], with an optional way past the page, such as Skip, before it.
 */
@Composable
fun GuideScreen(
    page: Int,
    pageCount: Int,
    heading: @Composable (page: Int) -> GuidePageHeading,
    nextText: String,
    onNextClick: () -> Unit,
    canGoNext: Boolean = true,
    skipText: String? = null,
    onSkipClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.(page: Int) -> Unit,
) {
    Scaffold(
        bottomBar = {
            BottomActionBar {
                InfoActions(
                    acceptText = nextText,
                    onAcceptClick = onNextClick,
                    canAccept = canGoNext,
                    rejectText = skipText,
                    onRejectClick = onSkipClick,
                )
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GuideProgress(page = page, pageCount = pageCount)
            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / SLIDE_FRACTION else -it / SLIDE_FRACTION } + fadeIn())
                        .togetherWith(
                            slideOutHorizontally { if (forward) -it / SLIDE_FRACTION else it / SLIDE_FRACTION } +
                                fadeOut(),
                        )
                },
                label = "guidePage",
            ) { shown ->
                val pageHeading = heading(shown)
                InfoPage(
                    icon = pageHeading.icon,
                    headingText = pageHeading.heading,
                    subtitleText = pageHeading.subtitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    topPadding = PaddingValues(top = KatariTheme.spacing.xl),
                    content = { content(shown) },
                )
            }
        }
    }
}

/** How far along the guide is, as a bar and "2 of 6". */
@Composable
private fun GuideProgress(page: Int, pageCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.l, top = KatariTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            modifier = Modifier.weight(1f),
            progress = { (page + 1f) / pageCount },
        )
        Text(
            text = stringResource(MR.strings.guide_page_of, page + 1, pageCount),
            role = TextRole.Supporting,
        )
    }
}

// The pages slide by a quarter of the width as they cross-fade.
private const val SLIDE_FRACTION = 4

@PreviewLightDark
@Composable
private fun GuideScreenPreview() {
    GuideScreen(
        page = 1,
        pageCount = 4,
        heading = {
            if (it == 0) {
                GuidePageHeading(MaterialSymbols.Rounded.RocketLaunch, "Welcome", "Subtitle")
            } else {
                GuidePageHeading(MaterialSymbols.Rounded.Palette, "Appearance", "Subtitle")
            }
        },
        nextText = "Next",
        onNextClick = {},
        skipText = "Skip",
        onSkipClick = {},
    ) {
        Text("Page $it")
    }
}

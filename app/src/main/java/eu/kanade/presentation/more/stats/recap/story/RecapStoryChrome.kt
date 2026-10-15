package eu.kanade.presentation.more.stats.recap.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.StoryPalette
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/*
 * The controls around a recap story. They sit inside the story's StoryTheme, so they take the current page's colours.
 */

/**
 * One bar per page: pages already seen are full, the current one fills as it plays.
 *
 * @param progress how far the current page has played, read on every frame while drawing only.
 */
@Composable
internal fun RecapProgressBars(
    count: Int,
    index: Int,
    progress: () -> Float,
    palette: StoryPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
        modifier = modifier.fillMaxWidth().height(StoryMetrics.progressHeight),
    ) {
        repeat(count) { page ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(KatariTheme.shapes.full)
                    .background(palette.inkTrack)
                    .drawBehind {
                        val filled = when {
                            page < index -> 1f
                            page == index -> progress().coerceIn(0f, 1f)
                            else -> 0f
                        }
                        drawRect(palette.ink, size = size.copy(width = size.width * filled))
                    },
            )
        }
    }
}

@Composable
internal fun RecapTopBar(
    title: String,
    onMore: () -> Unit,
    onClose: () -> Unit,
    moreMenu: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            tone = ContentTone.Secondary,
            modifier = Modifier.weight(1f).padding(start = KatariTheme.spacing.l),
        )
        Box {
            IconButton(
                icon = MaterialSymbols.Rounded.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu),
                onClick = onMore,
            )
            moreMenu()
        }
        IconButton(
            icon = MaterialSymbols.Rounded.Close,
            contentDescription = stringResource(MR.strings.action_close),
            onClick = onClose,
        )
    }
}

/** Share for any page; on the last page also a way to watch the story again. */
@Composable
internal fun RecapBottomBar(
    isLastPage: Boolean,
    canReplay: Boolean,
    onShare: () -> Unit,
    onReplay: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            KatariTheme.spacing.m,
            if (isLastPage) Alignment.CenterHorizontally else Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(StoryMetrics.controlsHeight)
            .padding(horizontal = KatariTheme.spacing.s),
    ) {
        if (isLastPage) {
            Button(
                label = stringResource(MR.strings.action_share),
                onClick = onShare,
                icon = MaterialSymbols.Rounded.Share,
            )
            if (canReplay) {
                Button(
                    label = stringResource(MR.strings.statistics_recap_watch_again),
                    onClick = onReplay,
                    style = ButtonStyle.Outlined,
                )
            }
        } else {
            IconButton(
                icon = MaterialSymbols.Rounded.Share,
                contentDescription = stringResource(MR.strings.action_share),
                onClick = onShare,
            )
        }
    }
}

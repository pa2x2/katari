package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.UnfoldMore
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What a gap row shows: pages still loading into it, why the last page failed, or that pages are left to load. */
internal sealed interface FeedGapStatus {
    data class Loading(val pagesLoaded: Int) : FeedGapStatus
    data class Failed(val message: String) : FeedGapStatus
    data object Idle : FeedGapStatus
}

/** The row between items a refresh loaded and older items it did not reach, spanning the feed's width. */
@Composable
internal fun FeedGapRow(
    status: FeedGapStatus,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KatariTheme.spacing.xs),
        container = SurfaceContainer.High,
    ) {
        Row(
            modifier = Modifier.padding(
                start = KatariTheme.spacing.l,
                end = KatariTheme.spacing.m,
                top = KatariTheme.spacing.m,
                bottom = KatariTheme.spacing.m,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status) {
                is FeedGapStatus.Loading -> LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
                is FeedGapStatus.Failed -> Icon(
                    imageVector = MaterialSymbols.Rounded.Error,
                    contentDescription = null,
                    tone = ContentTone.Error,
                )
                FeedGapStatus.Idle -> Icon(
                    imageVector = MaterialSymbols.Rounded.UnfoldMore,
                    contentDescription = null,
                    tone = ContentTone.Secondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
            ) {
                Text(
                    text = when (status) {
                        is FeedGapStatus.Loading -> stringResource(MR.strings.browse_feed_loading_newer_items)
                        is FeedGapStatus.Failed -> stringResource(MR.strings.browse_feed_gap_failed)
                        FeedGapStatus.Idle -> stringResource(MR.strings.browse_feed_gap_title)
                    },
                    role = TextRole.ItemTitle,
                )
                Text(
                    text = when (status) {
                        is FeedGapStatus.Loading -> stringResource(
                            MR.strings.browse_feed_gap_progress,
                            status.pagesLoaded,
                            FeedViewModel.CATCH_UP_PAGE_LIMIT,
                        )
                        is FeedGapStatus.Failed -> status.message
                        FeedGapStatus.Idle -> stringResource(
                            MR.strings.browse_feed_gap_summary,
                            FeedViewModel.CATCH_UP_PAGE_LIMIT,
                        )
                    },
                    role = TextRole.Supporting,
                    tone = ContentTone.Secondary,
                )
            }
            when (status) {
                is FeedGapStatus.Loading -> Unit
                is FeedGapStatus.Failed -> Button(
                    label = stringResource(MR.strings.action_retry),
                    onClick = onLoad,
                    style = ButtonStyle.Tonal,
                )
                FeedGapStatus.Idle -> Button(
                    label = stringResource(MR.strings.browse_feed_gap_load),
                    onClick = onLoad,
                    style = ButtonStyle.Tonal,
                )
            }
        }
    }
}

/** The line above the newest item the reader had already seen, spanning the feed's width. */
@Composable
internal fun FeedSeenDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(MR.strings.browse_feed_seen_before),
            role = TextRole.Supporting,
            tone = ContentTone.Secondary,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/** Shown at the end of the feed when loading older items failed, in place of the loading indicator. */
@Composable
internal fun FeedAppendErrorRow(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            role = TextRole.Supporting,
            tone = ContentTone.Secondary,
        )
        Button(
            label = stringResource(MR.strings.action_retry),
            onClick = onRetry,
            style = ButtonStyle.Text,
        )
    }
}

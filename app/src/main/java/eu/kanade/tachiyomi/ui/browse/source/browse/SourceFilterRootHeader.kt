package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.TonedSpan
import mihon.presentation.kit.content.TonedText
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What the root sheet reports about the draft on its single status line. */
internal data class SourceFilterSheetStatus(
    val changedCount: Int,
    val updating: Boolean,
    val unapplied: Boolean,
    val query: String?,
)

/**
 * The title with the preset chip, one status line that always occupies exactly one line so edits never shift the
 * list, and the All / Changed view toggle.
 */
@Composable
internal fun SourceFilterRootHeader(
    status: SourceFilterSheetStatus,
    changedOnly: Boolean,
    onChangedOnlyChange: (Boolean) -> Unit,
    presetChip: (@Composable () -> Unit)?,
) {
    val spacing = KatariTheme.spacing
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = spacing.rowInset, end = spacing.rowInset, top = spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Text(stringResource(MR.strings.filter_title), role = TextRole.SheetTitle)
            presetChip?.invoke()
        }
        SourceFilterStatusLine(status, Modifier.padding(horizontal = spacing.rowInset, vertical = spacing.xs))
        Row(
            modifier = Modifier.padding(horizontal = spacing.rowInset),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
        ) {
            FilterChip(
                label = stringResource(MR.strings.filter_view_all),
                selected = !changedOnly,
                onClick = { onChangedOnlyChange(false) },
            )
            FilterChip(
                label = stringResource(MR.strings.filter_view_changed, status.changedCount),
                selected = changedOnly,
                onClick = { onChangedOnlyChange(true) },
            )
        }
    }
}

/** The draft's state on one line: its changes or that it is updating, whether it is applied, and the search query. */
@Composable
private fun SourceFilterStatusLine(status: SourceFilterSheetStatus, modifier: Modifier) {
    val changed = if (status.changedCount > 0) {
        stringResource(MR.strings.filter_status_changed, status.changedCount)
    } else {
        stringResource(MR.strings.filter_status_defaults)
    }
    val updating = stringResource(MR.strings.filter_updating)
    val notApplied = stringResource(MR.strings.filter_status_not_applied)
    val query = status.query?.takeIf { it.isNotBlank() }?.let { stringResource(MR.strings.filter_query, it) }
    TonedText(
        spans = listOfNotNull(
            TonedSpan(if (status.updating) updating else changed),
            TonedSpan(notApplied, ContentTone.Primary).takeIf { status.unapplied },
            query?.let(::TonedSpan),
        ),
        modifier = modifier,
        role = TextRole.Supporting,
        separator = " · ",
        maxLines = 1,
        minLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

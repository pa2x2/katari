package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
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
    val horizontal = FilterSheetInsets.Horizontal
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = horizontal, end = horizontal, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(MR.strings.filter_title), style = MaterialTheme.typography.headlineSmall)
            presetChip?.invoke()
        }
        SourceFilterStatusLine(status, Modifier.padding(horizontal = horizontal, vertical = 4.dp))
        Row(
            modifier = Modifier.padding(horizontal = horizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !changedOnly,
                onClick = { onChangedOnlyChange(false) },
                label = { Text(stringResource(MR.strings.filter_view_all)) },
            )
            FilterChip(
                selected = changedOnly,
                onClick = { onChangedOnlyChange(true) },
                label = { Text(stringResource(MR.strings.filter_view_changed, status.changedCount)) },
            )
        }
    }
}

@Composable
private fun SourceFilterStatusLine(status: SourceFilterSheetStatus, modifier: Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val updating = stringResource(MR.strings.filter_updating)
    val changed = if (status.changedCount > 0) {
        stringResource(MR.strings.filter_status_changed, status.changedCount)
    } else {
        stringResource(MR.strings.filter_status_defaults)
    }
    val notApplied = stringResource(MR.strings.filter_status_not_applied)
    val query = status.query?.takeIf { it.isNotBlank() }?.let { stringResource(MR.strings.filter_query, it) }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = muted)) { append(if (status.updating) updating else changed) }
            if (status.unapplied) withStyle(SpanStyle(color = accent)) { append(" · $notApplied") }
            if (query != null) withStyle(SpanStyle(color = muted)) { append(" · $query") }
        },
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        minLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

package eu.kanade.presentation.entry.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.entry.olderchapters.OlderChaptersStatus
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.progress.ProgressSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Where a chapter list still arriving in parts gets its older chapters: how far reading them got, or why it stopped,
 * with Retry. It sits where they will appear, past the oldest chapter listed.
 */
fun LazyListScope.olderChaptersItem(status: OlderChaptersStatus?, onRetry: () -> Unit) {
    if (status == null) return
    item(key = "older-chapters") { OlderChaptersRow(status, onRetry) }
}

@Composable
private fun OlderChaptersRow(status: OlderChaptersStatus, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    RowGroupItem(position = RowGroupPosition.Alone, modifier = modifier) {
        val failure = status.failure
        val loaded = status.loaded
        val total = status.total
        when {
            failure != null -> SettingsRow(
                title = stringResource(MR.strings.older_chapters_stopped),
                subtitle = failure,
                subtitleMaxLines = 2,
                icon = MaterialSymbols.Rounded.CloudOff,
                trailing = {
                    Button(label = stringResource(MR.strings.action_retry), onClick = onRetry, style = ButtonStyle.Text)
                },
            )
            loaded != null && total != null && total > 0 -> SettingsRow(
                title = stringResource(MR.strings.older_chapters_loading_pages, loaded, total),
                leading = { CircularProgressIndicator(size = ProgressSize.Inline) },
                below = {
                    LinearProgressIndicator(progress = { (loaded.toFloat() / total).coerceIn(0f, 1f) })
                },
            )
            else -> SettingsRow(
                title = stringResource(MR.strings.older_chapters_loading),
                leading = { CircularProgressIndicator(size = ProgressSize.Inline) },
            )
        }
    }
}

package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Update
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.tachiyomi.data.library.label
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The update running into the report: how far it got and the entries it is checking now, or whose update it waits for,
 * with [onStop] to stop it. What it checked stays in the report either way.
 */
@Composable
internal fun ReportProgressCard(update: LibraryUpdateState, onStop: () -> Unit) {
    val stop: @Composable () -> Unit = {
        Button(label = stringResource(MR.strings.action_cancel), onClick = onStop, style = ButtonStyle.Text)
    }
    RowGroupItem(position = RowGroupPosition.Alone) {
        when (update) {
            is LibraryUpdateState.Running -> {
                val progress = update.progress
                val context = currentPlatformContext()
                SettingsRow(
                    title = if (progress.total > 0) {
                        stringResource(MR.strings.library_update_report_checking, progress.completed, progress.total)
                    } else {
                        remember(progress, context) { progress.label(context) }
                    },
                    subtitle = progress.checking.takeIf { it.isNotEmpty() }?.let {
                        stringResource(MR.strings.library_update_report_checking_now, it.joinToString(" · "))
                    },
                    subtitleMaxLines = 1,
                    icon = MaterialSymbols.Rounded.Update,
                    trailing = stop,
                    below = {
                        if (progress.total > 0) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(top = KatariTheme.spacing.s),
                                progress = { progress.completed.toFloat() / progress.total },
                            )
                        }
                    },
                )
            }
            is LibraryUpdateState.Waiting -> SettingsRow(
                title = stringResource(MR.strings.library_update_waiting, update.runningProfileName),
                icon = MaterialSymbols.Rounded.Update,
                trailing = stop,
            )
        }
    }
}

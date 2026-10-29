package eu.kanade.presentation.crash

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.screen.debug.rememberLogDump
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.BugReport
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.InfoScreen
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CrashScreen(
    exception: Throwable?,
    onRestartClick: () -> Unit,
) {
    val dumpLogs = rememberLogDump(exception)

    InfoScreen(
        icon = MaterialSymbols.Rounded.BugReport,
        headingText = stringResource(MR.strings.crash_screen_title),
        subtitleText = stringResource(MR.strings.crash_screen_report_description, stringResource(MR.strings.app_name)),
        acceptText = stringResource(MR.strings.pref_dump_crash_logs),
        onAcceptClick = dumpLogs,
        rejectText = stringResource(MR.strings.crash_screen_restart_application),
        onRejectClick = onRestartClick,
    ) {
        RowGroup(
            rows = listOf {
                ControlRow {
                    Text(text = exception.toString(), role = TextRole.Code, tone = ContentTone.Secondary)
                }
            },
        )
    }
}

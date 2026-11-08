package eu.kanade.presentation.crash

import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.BugReport
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.InfoScreen
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.settings.screen.debug.rememberLogDump
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

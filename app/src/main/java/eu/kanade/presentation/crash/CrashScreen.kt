package eu.kanade.presentation.crash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.BugReport
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.InfoScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CrashScreen(
    exception: Throwable?,
    onRestartClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    InfoScreen(
        icon = MaterialSymbols.Rounded.BugReport,
        headingText = stringResource(MR.strings.crash_screen_title),
        subtitleText = stringResource(MR.strings.crash_screen_report_description, stringResource(MR.strings.app_name)),
        acceptText = stringResource(MR.strings.pref_dump_crash_logs),
        onAcceptClick = {
            scope.launch {
                context.appGraph.crashLogUtilFactory.create(context).dumpLogs(exception)
            }
        },
        rejectText = stringResource(MR.strings.crash_screen_restart_application),
        onRejectClick = onRestartClick,
    ) {
        Surface(
            modifier = Modifier
                .padding(vertical = KatariTheme.spacing.s)
                .fillMaxSize(),
            container = SurfaceContainer.Highest,
            shape = KatariTheme.shapes.small,
        ) {
            Text(
                text = exception.toString(),
                modifier = Modifier.padding(all = KatariTheme.spacing.s),
                role = TextRole.Code,
                tone = ContentTone.Secondary,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CrashScreenPreview() {
    KatariPreviewTheme {
        CrashScreen(exception = RuntimeException("Dummy")) {}
    }
}

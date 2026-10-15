package eu.kanade.tachiyomi.ui.library.update.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import eu.kanade.presentation.library.update.report.LibraryUpdateReportContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The latest library update: what failed and why, what came in, and what was skipped or left out. */
class LibraryUpdateReportScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<LibraryUpdateReportViewModel>()
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // A started recheck shows up in this report as it runs, so only a refused start needs a message.
        fun reportStart(started: Boolean) {
            if (started) return
            scope.launch {
                snackbarHostState.showSnackbar(context.stringResource(MR.strings.update_already_running))
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.library_update_report),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            when (val current = state) {
                LibraryUpdateReportViewModel.State.Loading -> LoadingScreen()
                LibraryUpdateReportViewModel.State.Empty -> EmptyScreen(
                    stringRes = MR.strings.library_updates_last_update_none,
                )
                is LibraryUpdateReportViewModel.State.Ready -> LibraryUpdateReportContent(
                    state = current,
                    contentPadding = contentPadding,
                    actions = LibraryUpdateReportActions(
                        onClickEntry = { navigator.push(EntryScreen(it.id)) },
                        onMigrate = { navigator.push(MigrationConfigScreen(it)) },
                        onWebView = { entry, webView ->
                            navigator.push(
                                WebViewScreen(
                                    url = webView.url,
                                    initialTitle = entry.title,
                                    sourceId = webView.sourceId,
                                    headers = webView.headers,
                                ),
                            )
                        },
                        onSetPaused = viewModel::setEntryPaused,
                        onPauseSource = viewModel::pauseSource,
                        onResumeSource = viewModel::resumeSource,
                        onRetry = { entries -> scope.launch { reportStart(viewModel.retry(entries)) } },
                        onCheckSkipped = { scope.launch { reportStart(viewModel.checkSkipped()) } },
                    ),
                )
            }
        }
    }
}

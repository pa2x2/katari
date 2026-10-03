package eu.kanade.tachiyomi.ui.library.update.report

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import eu.kanade.presentation.library.update.report.LibraryUpdateReportContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/** The latest library update: what failed and why, what came in, and what was skipped or left out. */
class LibraryUpdateReportScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { LibraryUpdateReportScreenModel() }
        val state by screenModel.state.collectAsState()
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
                LibraryUpdateReportScreenModel.State.Loading -> LoadingScreen()
                LibraryUpdateReportScreenModel.State.Empty -> EmptyScreen(
                    stringRes = MR.strings.library_updates_last_update_none,
                )
                is LibraryUpdateReportScreenModel.State.Ready -> LibraryUpdateReportContent(
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
                        onSetPaused = screenModel::setEntryPaused,
                        onPauseSource = screenModel::pauseSource,
                        onResumeSource = screenModel::resumeSource,
                        onRetry = { entries -> scope.launch { reportStart(screenModel.retry(entries)) } },
                        onCheckSkipped = { scope.launch { reportStart(screenModel.checkSkipped()) } },
                    ),
                )
            }
        }
    }
}

package eu.kanade.tachiyomi.ui.library.update.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.library.update.rememberLibraryUpdateStarter
import eu.kanade.presentation.library.update.report.LibraryUpdateReportActions
import eu.kanade.presentation.library.update.report.LibraryUpdateReportContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The latest library update: what failed and why, what came in, and what was skipped or left out. */
class LibraryUpdateReportScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val graph = LocalAppUiGraph.current
        val migrationDestination = remember { graph.entryMigrationDestination }
        val webViewDestination = remember { graph.webViewDestination }
        val viewModel = metroViewModel<LibraryUpdateReportViewModel>()
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // A started recheck shows up in this report as it runs, so only a queued or refused start needs a message.
        val updateStarter = rememberLibraryUpdateStarter(snackbarHostState)

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.library_update_report),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            snackbarHostState = snackbarHostState,
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
                        onMigrate = { subjects: List<EntryMigrationSubject> ->
                            navigator.push(migrationDestination.screen(subjects))
                        }.takeIf { migrationDestination.isAvailable },
                        onWebView = { entry: Entry, webView: EntryWebViewResolution.Available ->
                            navigator.push(
                                webViewDestination.screen(
                                    url = webView.url,
                                    title = entry.title,
                                    sourceId = webView.sourceId,
                                    headers = webView.headers,
                                ),
                            )
                        }.takeIf { webViewDestination.isAvailable },
                        onSetPaused = viewModel::setEntryPaused,
                        onPauseSource = viewModel::pauseSource,
                        onResumeSource = viewModel::resumeSource,
                        onRetry = { entries -> scope.launch { updateStarter.start { viewModel.retry(entries) } } },
                        onCheckSkipped = { scope.launch { updateStarter.start(viewModel::checkSkipped) } },
                    ),
                )
            }
        }
    }
}

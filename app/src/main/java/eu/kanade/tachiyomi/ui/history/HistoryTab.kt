package eu.kanade.tachiyomi.ui.history

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.history.HistoryScreen
import eu.kanade.presentation.history.components.HistoryDeleteAllDialog
import eu.kanade.presentation.history.components.HistoryDeleteDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.entry.interactions.navigation.EntryContinueResult
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data object HistoryTab : Tab {

    private val snackbarHostState = SnackbarHostState()

    private val resumeLastReadEvent = Channel<Unit>()

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 2u,
            title = stringResource(MR.strings.history),
        )

    override suspend fun onReselect(navigator: Navigator) {
        resumeLastReadEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val entryTypePresentationFeature = remember { context.appGraph.entryTypePresentationFeature }
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<HistoryViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val dayDurations by viewModel.dayDurations.collectAsStateWithLifecycle()

        HistoryScreen(
            state = state,
            dayDurations = dayDurations,
            snackbarHostState = snackbarHostState,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onClickDay = { date ->
                navigator.push(HistoryActivityScreen(startLocalDate = date.toString(), endLocalDate = date.toString()))
            },
            onClickCover = { item ->
                scope.launch {
                    navigator.push(EntryScreen(viewModel.getVisibleEntryId(item.historyItem.entryId)))
                }
            },
            canResume = { item -> viewModel.canContinue(item.historyItem.entryType) },
            onClickResume = { item ->
                scope.launch {
                    val entry = viewModel.getEntryById(item.historyItem.entryId) ?: return@launch
                    viewModel.continueEntry(context, entry)
                }
            },
            onClickDelete = { history -> viewModel.setDialog(HistoryViewModel.Dialog.Delete(history)) },
            onClickFavorite = { item -> viewModel.addFavorite(item.historyItem.entryId) },
            onDialogChange = viewModel::setDialog,
        )

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is HistoryViewModel.Dialog.Delete -> {
                HistoryDeleteDialog(
                    onDismissRequest = onDismissRequest,
                    onDelete = { all -> viewModel.removeFromHistory(dialog.history, all) },
                )
            }
            is HistoryViewModel.Dialog.DeleteAll -> {
                HistoryDeleteAllDialog(
                    onDismissRequest = onDismissRequest,
                    onDelete = viewModel::removeAllHistory,
                )
            }
            is HistoryViewModel.Dialog.DuplicateEntry -> {
                DuplicateEntryDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.addFavorite(dialog.entry) },
                    onOpenEntry = {
                        scope.launch {
                            navigator.push(EntryScreen(viewModel.getVisibleEntryId(it.id)))
                        }
                    },
                    onMigrate = { viewModel.showMigrateDialog(dialog.entry, it) },
                )
            }
            is HistoryViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.moveEntryToCategoriesAndAddToLibrary(dialog.entry, include)
                    },
                )
            }
            is HistoryViewModel.Dialog.Migrate -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.target] so we show [dialog.current].
                    onClickTitle = {
                        scope.launch {
                            navigator.push(EntryScreen(viewModel.getVisibleEntryId(dialog.current.id)))
                        }
                    },
                    onDismissRequest = onDismissRequest,
                )
            }
            null -> {}
        }

        LaunchedEffect(state.list) {
            if (state.list != null) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { e ->
                when (e) {
                    HistoryViewModel.Event.InternalError ->
                        snackbarHostState.showSnackbar(context.stringResource(MR.strings.internal_error))
                    HistoryViewModel.Event.HistoryCleared ->
                        snackbarHostState.showSnackbar(context.stringResource(MR.strings.clear_history_completed))
                }
            }
        }

        LaunchedEffect(Unit) {
            resumeLastReadEvent.receiveAsFlow().collectLatest {
                val history = viewModel.getMostRecentItem()?.historyItem
                if (history == null) {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_item))
                    return@collectLatest
                }

                val entry = viewModel.getEntryById(history.entryId)
                if (entry == null) {
                    snackbarHostState.showSnackbar(
                        context.stringResource(
                            history.entryType.entryTypePresentation(entryTypePresentationFeature).noNextChildLabel,
                        ),
                    )
                    return@collectLatest
                }

                when (viewModel.continueEntry(context, entry)) {
                    EntryContinueResult.Inapplicable -> {
                        snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_item))
                    }
                    EntryContinueResult.NoNext -> {
                        snackbarHostState.showSnackbar(
                            context.stringResource(
                                entry.type.entryTypePresentation(entryTypePresentationFeature).noNextChildLabel,
                            ),
                        )
                    }
                    is EntryContinueResult.Opened -> Unit
                }
            }
        }
    }
}

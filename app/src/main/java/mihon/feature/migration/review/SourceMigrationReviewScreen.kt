package mihon.feature.migration.review

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.feature.migration.review.components.SourceMigrationDiscardDialog
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.feature.migration.work.SourceMigrationNotificationVisibility
import mihon.feature.migration.work.SourceMigrationNotifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

class SourceMigrationReviewScreen(
    private val sessionId: SourceMigrationSessionId,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModel = viewModel { SourceMigrationReviewViewModel(sessionId) }
        val state by viewModel.state.collectAsState()
        val session = state.session
        val discoveryActive = session?.stage in DISCOVERY_STAGES
        val preparationActive = session?.stage in PREPARATION_STAGES
        val lazyListState = rememberLazyListState()
        var candidateItemId by rememberSaveable { mutableStateOf<Long?>(null) }
        var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

        DisposableEffect(lifecycleOwner, sessionId) {
            var visible = false
            fun show() {
                if (visible) return
                visible = true
                SourceMigrationNotificationVisibility.show(sessionId)
                SourceMigrationNotifier(context).cancel(sessionId)
            }
            fun hide() {
                if (!visible) return
                visible = false
                SourceMigrationNotificationVisibility.hide(sessionId)
            }

            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) = show()
                override fun onPause(owner: LifecycleOwner) = hide()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) show()
            onDispose {
                hide()
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(state.discardCompleted) {
            if (state.discardCompleted) navigator.pop()
        }

        LaunchedEffect(session?.stage) {
            when (session?.stage) {
                SourceMigrationSessionStage.DRAFT -> viewModel.startDiscovery()
                SourceMigrationSessionStage.COMPLETED -> navigator.pop()
                else -> Unit
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(
                        if (discoveryActive) {
                            MR.strings.sourceMigration_findingReplacements
                        } else {
                            MR.strings.sourceMigrationReview_title
                        },
                    ),
                    subtitle = session?.let {
                        stringResource(
                            MR.strings.sourceMigrationReview_subtitle,
                            state.originSourceName,
                            it.items.size,
                        )
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        val actions = buildList {
                            when (session?.stage) {
                                SourceMigrationSessionStage.DISCOVERY_QUEUED,
                                SourceMigrationSessionStage.DISCOVERING,
                                -> add(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_pause),
                                        icon = MaterialSymbols.RoundedFilled.Pause,
                                        onClick = viewModel::pauseDiscovery,
                                    ),
                                )
                                SourceMigrationSessionStage.DISCOVERY_PAUSED -> add(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_resume),
                                        icon = MaterialSymbols.Rounded.PlayArrow,
                                        onClick = viewModel::resumeDiscovery,
                                    ),
                                )
                                SourceMigrationSessionStage.EXECUTION_QUEUED,
                                SourceMigrationSessionStage.EXECUTING,
                                -> add(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_pause),
                                        icon = MaterialSymbols.RoundedFilled.Pause,
                                        onClick = viewModel::pauseExecution,
                                    ),
                                )
                                SourceMigrationSessionStage.EXECUTION_PAUSED -> add(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_resume),
                                        icon = MaterialSymbols.Rounded.PlayArrow,
                                        onClick = viewModel::resumeExecution,
                                    ),
                                )
                                else -> Unit
                            }
                            if (
                                session?.stage == SourceMigrationSessionStage.REVIEW_REQUIRED &&
                                state.unresolvedCount > 0
                            ) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.sourceMigrationReview_searchUnresolved),
                                        onClick = viewModel::retryUnresolved,
                                    ),
                                )
                            }
                            if (preparationActive) {
                                add(
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.sourceMigrationReview_discardAction),
                                        onClick = { showDiscardDialog = true },
                                    ),
                                )
                            }
                        }
                        AppBarActions(actions)
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    label = stringResource(
                        MR.strings.sourceMigrationReview_migrateCount,
                        session?.includedReadyCount ?: 0,
                    ),
                    icon = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                    onClick = viewModel::startExecution,
                    expanded = lazyListState.shouldExpandFAB(),
                    visible = session?.stage == SourceMigrationSessionStage.REVIEW_REQUIRED &&
                        session.includedReadyCount > 0,
                )
            },
        ) { contentPadding ->
            when {
                !state.isLoaded -> LoadingScreen()
                session == null -> EmptyScreen(MR.strings.internal_error)
                else -> SourceMigrationReviewContent(
                    state = state,
                    lazyListState = lazyListState,
                    contentPadding = contentPadding,
                    onFilterChange = viewModel::setFilter,
                    onIncludedChange = viewModel::setIncluded,
                    onToggleGroup = viewModel::toggleGroup,
                    onTargetClick = { sourceEntryId -> candidateItemId = sourceEntryId },
                    onSourceDetailsClick = { entryId ->
                        navigator.push(EntryScreen(entryId, bypassMerge = true))
                    },
                    onTargetDetailsClick = { entryId ->
                        navigator.push(EntryScreen(entryId, fromSource = true))
                    },
                )
            }
        }

        val candidateItem = session?.items?.firstOrNull { item -> item.sourceEntryId == candidateItemId }
        candidateItem?.let { item ->
            SourceMigrationCandidateSheet(
                sessionId = sessionId,
                item = item,
                onDismissRequest = { candidateItemId = null },
            )
        }

        if (showDiscardDialog) {
            SourceMigrationDiscardDialog(
                onDismissRequest = { showDiscardDialog = false },
                onConfirm = {
                    showDiscardDialog = false
                    viewModel.discardPreparation()
                },
            )
        }
    }
}

private val DISCOVERY_STAGES = setOf(
    SourceMigrationSessionStage.DISCOVERY_QUEUED,
    SourceMigrationSessionStage.DISCOVERING,
    SourceMigrationSessionStage.DISCOVERY_PAUSED,
)

private val PREPARATION_STAGES = DISCOVERY_STAGES + setOf(
    SourceMigrationSessionStage.DRAFT,
    SourceMigrationSessionStage.REVIEW_REQUIRED,
)

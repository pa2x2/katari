package mihon.feature.migration.config

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.config.MigrationConfigViewModel.SelectionConfig
import mihon.feature.migration.config.components.MigrationConfigSourceRow
import mihon.feature.migration.list.MigrationListScreen
import mihon.feature.migration.review.SourceMigrationReviewScreen
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSessionGroupDraft
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.Keep
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

class MigrationConfigScreen private constructor(
    private val subjects: Collection<EntryMigrationSubject>,
    private val sessionGroups: List<SourceMigrationSessionGroupDraft>?,
    private val originSourceId: Long?,
) : Screen() {

    constructor(subjects: Collection<EntryMigrationSubject>) : this(subjects, null, null)

    constructor(subject: EntryMigrationSubject) : this(listOf(subject), null, null)

    constructor(groups: List<SourceMigrationSessionGroupDraft>, originSourceId: Long) :
        this(emptyList(), groups, originSourceId)

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val isSourceSession = sessionGroups != null && originSourceId != null
        val viewModel = assistedMetroViewModel<MigrationConfigViewModel, MigrationConfigViewModel.Factory> {
            create(
                requiredEntryTypes = sessionGroups.orEmpty()
                    .flatMap(SourceMigrationSessionGroupDraft::members)
                    .filter { entry -> sessionGroups.orEmpty().any { entry.id in it.selectedEntryIds } }
                    .mapTo(mutableSetOf()) { it.type },
                excludedSourceId = originSourceId,
                persistSourceSelection = !isSourceSession,
            )
        }
        val state by viewModel.state.collectAsState()

        LaunchedEffect(state.createdSessionId) {
            state.createdSessionId?.let { sessionId ->
                navigator.popUntilRoot()
                navigator.push(SourceMigrationReviewScreen(sessionId))
            }
        }

        var migrationSheetOpen by rememberSaveable { mutableStateOf(false) }

        fun continueMigration(
            openSheet: Boolean,
            extraSearchQuery: String?,
            selectedOptions: Set<EntryMigrationOption> = emptySet(),
            initialDiscoveryDepth: SourceMigrationDiscoveryDepth = SourceMigrationDiscoveryDepth.STANDARD,
        ) {
            if (isSourceSession) {
                if (openSheet) {
                    migrationSheetOpen = true
                } else {
                    viewModel.startSession(
                        groups = requireNotNull(sessionGroups),
                        originSourceId = requireNotNull(originSourceId),
                        selectedOptions = selectedOptions,
                        initialDiscoveryDepth = initialDiscoveryDepth,
                    )
                }
                return
            }
            val subject = subjects.singleOrNull()
            if (subject == null && openSheet) {
                migrationSheetOpen = true
                return
            }
            val screen = if (subject == null) {
                MigrationListScreen(subjects, extraSearchQuery, selectedOptions)
            } else {
                MigrateSearchScreen(subject)
            }
            navigator.replace(screen)
        }

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        val (selectedSources, availableSources) = state.sources.partition { it.isSelected }
        val showLanguage by remember(state) {
            derivedStateOf {
                state.sources.distinctBy { it.source.lang }.size > 1
            }
        }

        val lazyListState = rememberLazyListState()
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.migrationConfigScreen_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectAllLabel),
                                    icon = MaterialSymbols.Rounded.SelectAll,
                                    onClick = { viewModel.toggleSelection(SelectionConfig.All) },
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectNoneLabel),
                                    icon = MaterialSymbols.Rounded.Deselect,
                                    onClick = { viewModel.toggleSelection(SelectionConfig.None) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectEnabledLabel),
                                    onClick = { viewModel.toggleSelection(SelectionConfig.Enabled) },
                                    icon = MaterialSymbols.Rounded.SelectAll,
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectPinnedLabel),
                                    onClick = { viewModel.toggleSelection(SelectionConfig.Pinned) },
                                    icon = MaterialSymbols.Rounded.Keep,
                                ),
                            ),
                        )
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    label = stringResource(MR.strings.migrationConfigScreen_continueButtonText),
                    icon = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                    onClick = {
                        if (state.selectedSourceIds.isNotEmpty()) {
                            viewModel.saveSources()
                            continueMigration(openSheet = true, extraSearchQuery = null)
                        }
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                )
            },
        ) { contentPadding ->
            val reorderableState = rememberReorderableLazyListState(lazyListState, contentPadding) { from, to ->
                val fromIndex = selectedSources.indexOfFirst { it.id == from.key }
                val toIndex = selectedSources.indexOfFirst { it.id == to.key }
                if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
                viewModel.orderSource(fromIndex, toIndex)
            }

            RowList(
                fastScroll = true,
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding,
            ) {
                listOf(selectedSources, availableSources).fastForEachIndexed { listIndex, sources ->
                    val selectedSourceList = listIndex == 0
                    if (sources.isEmpty()) return@fastForEachIndexed
                    val headerPrefix = if (selectedSourceList) "selected" else "available"
                    if (!selectedSourceList && selectedSources.isNotEmpty()) {
                        item("$headerPrefix-gap") { RowGroupGap() }
                    }
                    item("$headerPrefix-header") {
                        RowGroupHeader(
                            text = stringResource(
                                resource = if (selectedSourceList) {
                                    MR.strings.migrationConfigScreen_selectedHeader
                                } else {
                                    MR.strings.migrationConfigScreen_availableHeader
                                },
                            ),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    itemsIndexed(
                        items = sources,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        MigrationConfigSourceRow(
                            source = item,
                            position = RowGroupPosition.of(index, sources.size),
                            showLanguage = showLanguage,
                            dragEnabled = selectedSourceList && sources.size > 1,
                            state = reorderableState,
                            key = if (selectedSourceList) item.id else "available-${item.id}",
                            onClick = { viewModel.toggleSelection(item.id) },
                        )
                    }
                }
            }
        }

        if (migrationSheetOpen) {
            MigrationConfigScreenSheet(
                preferences = viewModel.sourcePreferences,
                showSearchOptions = !isSourceSession,
                showInitialAdvancedSearchOption = isSourceSession,
                onDismissRequest = { migrationSheetOpen = false },
                onStartMigration = { extraSearchQuery, selectedOptions, initialDiscoveryDepth ->
                    migrationSheetOpen = false
                    continueMigration(
                        openSheet = false,
                        extraSearchQuery = extraSearchQuery,
                        selectedOptions = selectedOptions,
                        initialDiscoveryDepth = initialDiscoveryDepth,
                    )
                },
            )
        }
    }
}

package eu.kanade.tachiyomi.ui.browse.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.toListing
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreen
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.catalog.BrowseLongPressOutcome
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.browse.feed.add.AddFeedSheet
import eu.kanade.tachiyomi.ui.browse.feed.manage.ManageFeedsSheet
import eu.kanade.tachiyomi.ui.browse.feed.manage.RenameFeedDialog
import eu.kanade.tachiyomi.ui.browse.feed.switcher.FeedNavigationBar
import eu.kanade.tachiyomi.ui.browse.feed.switcher.FeedPickerSheet
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.entry.interactions.media.EntryImmersiveSourceAvailability
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.FloatingToolbarBox
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

@Composable
fun Screen.feedsTab(contentMode: SourceFeedContentMode = SourceFeedContentMode.Browse): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val profileManager = remember { context.appGraph.profileManager }
    val activeProfile by profileManager.activeProfile.collectAsState()
    val activeProfileId = activeProfile?.id ?: profileManager.activeProfileId
    val viewModel = assistedMetroViewModel<FeedsViewModel, FeedsViewModel.Factory> { create(contentMode) }
    val state by viewModel.state.collectAsState()
    val singleEnabledFeed = state.enabledFeeds.singleOrNull()
    val singleEnabledFeedSource = singleEnabledFeed?.let { viewModel.sourceFor(it.sourceId) }
    val singleEnabledFeedPreset = singleEnabledFeed?.let(viewModel::presetFor)
    val activeFeedSource = viewModel.activeFeed()?.let { viewModel.sourceFor(it.sourceId) }
    var feedViewMode by remember { mutableStateOf(FeedViewMode.Regular) }

    return TabContent(
        titleRes = MR.strings.browse_feeds,
        chromeVisible = { feedViewMode == FeedViewMode.Regular },
        tabLabel = if (
            singleEnabledFeed != null && singleEnabledFeedSource != null && singleEnabledFeedPreset != null
        ) {
            {
                SingleFeedTabLabel(
                    label = rememberFeedLabel(singleEnabledFeed, singleEnabledFeedSource, singleEnabledFeedPreset),
                    source = singleEnabledFeedSource,
                )
            }
        } else {
            null
        },
        actions = buildList {
            add(
                AppBar.Action(
                    title = stringResource(MR.strings.browse_feed_add),
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = viewModel::showCreateDialog,
                ),
            )
            add(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.browse_manage_feeds),
                    onClick = viewModel::showManageDialog,
                ),
            )
            if (activeFeedSource != null) {
                add(
                    AppBar.OverflowAction(
                        title = stringResource(
                            MR.strings.browse_feed_long_press_actions_for_source,
                            activeFeedSource.name,
                        ),
                        onClick = { navigator.push(BrowseLongPressActionsScreen(activeFeedSource.id)) },
                    ),
                )
            }
        },
        content = { contentPadding, snackbarHostState ->
            FeedsTabContent(
                activeProfileId = activeProfileId,
                state = state,
                viewModel = viewModel,
                navigator = navigator,
                contentPadding = contentPadding,
                snackbarHostState = snackbarHostState,
                contentMode = contentMode,
                feedViewMode = feedViewMode,
                onFeedViewModeChange = { feedViewMode = it },
            )
        },
    )
}

internal enum class FeedViewMode {
    Regular,
    Immersive,
}

@Composable
private fun SingleFeedTabLabel(
    label: FeedLabel,
    source: Source,
) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        SourceIcon(source = source, size = LogoSize.Inline)
        // A tab has no room for the subtitle; the feed bar below shows the full label.
        Text(
            text = label.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Screen.FeedsTabContent(
    activeProfileId: Long,
    state: FeedsViewModel.State,
    viewModel: FeedsViewModel,
    navigator: Navigator,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    contentMode: SourceFeedContentMode,
    feedViewMode: FeedViewMode,
    onFeedViewModeChange: (FeedViewMode) -> Unit,
) {
    if (!state.sourcesLoaded) {
        LoadingScreen()
        return
    }

    val activeFeed = viewModel.activeFeed()
    val activeSource = activeFeed?.let { viewModel.sourceFor(it.sourceId) }
    val activePreset = activeFeed?.let(viewModel::presetFor)
    var renamingFeedId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val graph = remember { context.appGraph }
    val sourceManager = remember { graph.sourceManager }
    val immersiveFeature = remember { graph.entryImmersiveFeature }
    val immersiveAvailable = immersiveFeature.sourceAvailability(
        activeSource?.let { sourceManager.get(it.id) },
    ) is EntryImmersiveSourceAvailability.Available
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    LaunchedEffect(activeFeed?.id, immersiveAvailable) {
        if (!immersiveAvailable && feedViewMode == FeedViewMode.Immersive) {
            onFeedViewModeChange(FeedViewMode.Regular)
        }
    }

    LaunchedEffect(feedViewMode) {
        HomeScreen.showBottomNav(feedViewMode == FeedViewMode.Regular)
    }

    BackHandler(enabled = feedViewMode == FeedViewMode.Immersive) {
        onFeedViewModeChange(FeedViewMode.Regular)
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch { HomeScreen.showBottomNav(true) }
        }
    }

    val openItem: (CatalogListItem) -> Unit = { item ->
        when (item) {
            is CatalogListItem.EntryItem -> {
                navigator.push(EntryScreen(item.entry.id, fromSource = true))
            }
        }
    }

    if (state.enabledFeeds.isEmpty()) {
        EmptyScreen(
            message = stringResource(MR.strings.browse_feeds_empty) + "\n" +
                stringResource(MR.strings.browse_feeds_empty_summary),
            modifier = Modifier.padding(contentPadding),
            actions = listOf(
                EmptyScreenAction(
                    stringRes = MR.strings.browse_feed_add,
                    icon = MaterialSymbols.Rounded.Add,
                    onClick = viewModel::showCreateDialog,
                ),
            ),
        )
    } else if (activeFeed != null && activeSource != null && activePreset != null) {
        val enabledFeeds = state.enabledFeeds
        val activeDisplayMode = viewModel.displayModeFor(
            activeFeed,
            viewModel.sourceDisplayMode(activeFeed.sourceId),
        )
        var showFeedPicker by remember(activeFeed.id) { mutableStateOf(false) }
        var jumpToNewestRequest by remember(activeFeed.id) { mutableIntStateOf(0) }

        Column(
            modifier = Modifier.pointerInput(Unit) {},
        ) {
            key(activeProfileId, activeFeed.id, activePreset) {
                val actionModel = assistedMetroViewModel<CatalogViewModel, CatalogViewModel.Factory>(
                    key = "feed-actions-$activeProfileId-${activeFeed.id}-${activePreset.hashCode()}",
                ) {
                    create(
                        sourceId = activeSource.id,
                        listingQuery = activePreset.toListing().requestQuery,
                        migrationEntryType = null,
                        initialFilterSnapshot = activePreset.filters,
                        initialPresetId = activePreset.id,
                        openFilterSheetOnStart = false,
                        feedProfileId = activeProfileId,
                    )
                }
                val actionState by actionModel.state.collectAsState()
                val timelineModel =
                    viewModel(key = "feed-timeline-$activeProfileId-${activeFeed.id}-${activePreset.hashCode()}") {
                        FeedViewModel(
                            feedId = activeFeed.id,
                            initialFilterSnapshot = activePreset.filters,
                            chronological = activePreset.chronological,
                            browseFeedService = graph.browseFeedService.forProfile(activeProfileId),
                            hideInLibraryItems = graph.profileSourcePreferences
                                .forProfile(activeProfileId).hideInLibraryItems.get(),
                            source = graph.catalogFeedItemSourceFactory.create(
                                sourceId = activeSource.id,
                                listingQuery = activePreset.toListing().requestQuery,
                                initialFilterSnapshot = activePreset.filters,
                            ),
                        )
                    }
                val timelineState by timelineModel.state.collectAsState()
                val immersiveModel = metroViewModel<EntryImmersiveViewModel>(
                    key = "feed-immersive-$activeProfileId-${activeFeed.id}",
                )
                val catalogSource = actionModel.catalogSource
                val sourceItemOrientation = catalogSource?.itemOrientation
                    ?: EntryItemOrientation.VERTICAL
                val columns = remember(configuration.orientation, sourceItemOrientation) {
                    val isLandscape = configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val portraitColumns = 3
                    val landscapeColumns = 5
                    val columns = if (isLandscape) landscapeColumns else portraitColumns
                    if (columns == 0) {
                        androidx.compose.foundation.lazy.grid.GridCells.Adaptive(
                            if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) {
                                BrowseMetrics.wideCoverGridMinCellWidth
                            } else {
                                BrowseMetrics.coverGridMinCellWidth
                            },
                        )
                    } else {
                        androidx.compose.foundation.lazy.grid.GridCells.Fixed(
                            if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) {
                                (columns - 1).coerceAtLeast(1)
                            } else {
                                columns
                            },
                        )
                    }
                }

                if (actionState.repairNeedsSave) {
                    FeedPresetRepairNotice(onRepair = actionModel::openFilterSheet)
                }
                if (actionState.dialog == CatalogViewModel.Dialog.Filter) {
                    FeedPresetRepairSheet(actionModel, actionState)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (feedViewMode == FeedViewMode.Immersive) {
                        EntryImmersiveFeedContent(
                            timelineModel = timelineModel,
                            immersiveModel = immersiveModel,
                            snackbarHostState = snackbarHostState,
                            activeSource = activeSource,
                            feedLabel = rememberFeedLabel(activeFeed, activeSource, activePreset),
                            onShowFeedPicker = { showFeedPicker = true },
                            onExitImmersive = { onFeedViewModeChange(FeedViewMode.Regular) },
                            onEntryClick = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                            onLibraryAction = actionModel::confirmBrowseLibraryAction,
                            onPagingBlockedChange = {},
                            jumpToNewestRequest = jumpToNewestRequest,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        FloatingToolbarBox(
                            bottomPadding = contentPadding.calculateBottomPadding(),
                            toolbar = {
                                FeedNavigationBar(
                                    feeds = enabledFeeds,
                                    selectedFeed = activeFeed,
                                    selectedDisplayMode = activeDisplayMode,
                                    sourceFor = viewModel::sourceFor,
                                    presetFor = viewModel::presetFor,
                                    immersiveAvailable = immersiveAvailable,
                                    onFeedViewModeChange = onFeedViewModeChange,
                                    onDisplayModeChange = { viewModel.updateFeedDisplayMode(activeFeed.id, it) },
                                    onFeedSelect = viewModel::selectFeed,
                                    onRenameFeed = { renamingFeedId = activeFeed.id },
                                    onAddFeed = viewModel::showCreateDialog,
                                    onManageFeeds = viewModel::showManageDialog,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) { toolbarPadding ->
                            PullRefresh(
                                refreshing = timelineState.isRefreshing,
                                enabled = true,
                                onRefresh = { timelineModel.refresh() },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                CatalogFeedBrowseContent(
                                    source = catalogSource,
                                    viewModel = timelineModel,
                                    columns = columns,
                                    displayMode = activeDisplayMode,
                                    snackbarHostState = snackbarHostState,
                                    contentPadding = toolbarPadding,
                                    onWebViewClick = {
                                        val source = catalogSource
                                        val homeUrl = actionModel.homeUrl
                                        if (source != null && homeUrl != null) {
                                            navigator.push(
                                                WebViewScreen(
                                                    url = homeUrl,
                                                    initialTitle = source.name,
                                                    sourceId = source.id,
                                                ),
                                            )
                                        }
                                    },
                                    onHelpClick = { uriHandler.openUri(Constants.URL_HELP_TROUBLESHOOTING) },
                                    onLocalSourceHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) },
                                    onItemClick = openItem,
                                    onItemLongClick = { item ->
                                        scope.launch {
                                            val outcome = withIOContext {
                                                actionModel.onItemLongClick(item)
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (outcome == BrowseLongPressOutcome.StartImmersive) {
                                                timelineModel.saveAnchor(
                                                    itemRef = FeedItemRef(item.id, item.entryType),
                                                    scrollOffset = 0,
                                                )
                                                onFeedViewModeChange(FeedViewMode.Immersive)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                if (showFeedPicker && feedViewMode == FeedViewMode.Immersive) {
                    FeedPickerSheet(
                        feeds = enabledFeeds,
                        selectedFeedId = activeFeed.id,
                        sourceFor = viewModel::sourceFor,
                        presetFor = viewModel::presetFor,
                        canJumpToNewest = timelineState.itemRefs.isNotEmpty() &&
                            timelineModel.savedAnchorSnapshot().resolvedItem() != timelineState.itemRefs.firstOrNull(),
                        onSelect = { feedId ->
                            showFeedPicker = false
                            viewModel.selectFeed(feedId)
                        },
                        onRefresh = {
                            showFeedPicker = false
                            timelineModel.refresh()
                        },
                        onJumpToNewest = {
                            showFeedPicker = false
                            jumpToNewestRequest++
                        },
                        onAddFeed = {
                            showFeedPicker = false
                            viewModel.showCreateDialog()
                        },
                        onManageFeeds = {
                            showFeedPicker = false
                            viewModel.showManageDialog()
                        },
                        onDismissRequest = { showFeedPicker = false },
                    )
                }

                FeedCatalogActionDialogs(
                    dialog = actionState.dialog,
                    viewModel = actionModel,
                    navigator = navigator,
                )
            }
        }
    }

    when (state.dialog) {
        FeedsViewModel.Dialog.AddFeed -> {
            AddFeedSheet(
                sources = state.listedSources,
                feeds = state.validFeeds,
                presetsFor = viewModel::presetsFor,
                onSelectPreset = { source, preset -> viewModel.createFeed(source.id, preset.id) },
                onCreateFilteredFeed = { source ->
                    viewModel.closeDialog()
                    navigator.push(CatalogScreen(source.id, listingQuery = null, openFilters = true))
                },
                onDismissRequest = viewModel::closeDialog,
            )
        }
        FeedsViewModel.Dialog.ManageFeeds -> {
            ManageFeedsSheet(
                feeds = state.validFeeds,
                sourceFor = viewModel::sourceFor,
                presetFor = viewModel::presetFor,
                onReorder = viewModel::reorderFeed,
                onToggle = viewModel::toggleFeed,
                onRename = { renamingFeedId = it.id },
                onRemove = viewModel::removeFeed,
                onRestore = viewModel::restoreFeed,
                onAddFeed = viewModel::showCreateDialog,
                onDismissRequest = viewModel::closeDialog,
            )
        }
        null -> Unit
    }

    val renamingFeed = renamingFeedId?.let { feedId -> state.feeds.firstOrNull { it.id == feedId } }
    val renamingPreset = renamingFeed?.let(viewModel::presetFor)
    if (renamingFeed != null && renamingPreset != null) {
        RenameFeedDialog(
            currentTitle = renamingFeed.title,
            presetName = renamingPreset.displayName(),
            onDismissRequest = { renamingFeedId = null },
            onConfirm = { title ->
                renamingFeedId = null
                viewModel.renameFeed(renamingFeed.id, title)
            },
        )
    }
}

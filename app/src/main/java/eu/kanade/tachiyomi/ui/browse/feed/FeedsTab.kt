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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.toListing
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreen
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.browse.catalog.BrowseLongPressOutcome
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenModel
import eu.kanade.tachiyomi.ui.browse.feed.add.AddFeedSheet
import eu.kanade.tachiyomi.ui.browse.feed.manage.ManageFeedsSheet
import eu.kanade.tachiyomi.ui.browse.feed.manage.RenameFeedDialog
import eu.kanade.tachiyomi.ui.browse.feed.switcher.FeedNavigationBar
import eu.kanade.tachiyomi.ui.browse.feed.switcher.FeedPickerSheet
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.catalogue.EntryCatalogueSourceResolution
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.media.EntryImmersiveSourceAvailability
import mihon.entry.interactions.source.EntrySourceHomeFeature
import mihon.entry.interactions.source.EntrySourceHomeResolution
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun Screen.feedsTab(contentMode: SourceFeedContentMode = SourceFeedContentMode.Browse): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val profileManager = remember { Injekt.get<ProfileManager>() }
    val activeProfile by profileManager.activeProfile.collectAsState()
    val activeProfileId = activeProfile?.id ?: profileManager.activeProfileId
    val screenModel = rememberScreenModel { FeedsScreenModel(contentMode) }
    val state by screenModel.state.collectAsState()
    val singleEnabledFeed = state.enabledFeeds.singleOrNull()
    val singleEnabledFeedSource = singleEnabledFeed?.let { screenModel.sourceFor(it.sourceId) }
    val singleEnabledFeedPreset = singleEnabledFeed?.let(screenModel::presetFor)
    val activeFeedSource = screenModel.activeFeed()?.let { screenModel.sourceFor(it.sourceId) }
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
                    icon = Icons.Outlined.Add,
                    onClick = screenModel::showCreateDialog,
                ),
            )
            add(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.browse_manage_feeds),
                    onClick = screenModel::showManageDialog,
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
                screenModel = screenModel,
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SourceIcon(
            source = source,
            modifier = Modifier
                .size(18.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        // A fixed-width tab has no room for the subtitle; the feed bar below shows the full label.
        Text(
            text = label.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun Screen.FeedsTabContent(
    activeProfileId: Long,
    state: FeedsScreenModel.State,
    screenModel: FeedsScreenModel,
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

    val activeFeed = screenModel.activeFeed()
    val activeSource = activeFeed?.let { screenModel.sourceFor(it.sourceId) }
    val activePreset = activeFeed?.let(screenModel::presetFor)
    var renamingFeedId by rememberSaveable { mutableStateOf<String?>(null) }
    val sourceManager = remember { Injekt.get<SourceManager>() }
    val immersiveFeature = remember { Injekt.get<EntryImmersiveFeature>() }
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
                    icon = Icons.Outlined.Add,
                    onClick = screenModel::showCreateDialog,
                ),
            ),
        )
    } else if (activeFeed != null && activeSource != null && activePreset != null) {
        val enabledFeeds = state.enabledFeeds
        val activeIndex = remember(enabledFeeds, activeFeed.id) {
            enabledFeeds.indexOfFirst { it.id == activeFeed.id }
        }
        val activeDisplayMode = screenModel.displayModeFor(
            activeFeed,
            screenModel.sourceDisplayMode(activeFeed.sourceId),
        )
        val hasPreviousFeed = activeIndex > 0
        val hasNextFeed = activeIndex in 0 until enabledFeeds.lastIndex
        var showFeedPicker by remember(activeFeed.id) { mutableStateOf(false) }
        var jumpToNewestRequest by remember(activeFeed.id) { mutableIntStateOf(0) }

        Column(
            modifier = Modifier.pointerInput(Unit) {},
        ) {
            key(activeProfileId, activeFeed.id, activePreset) {
                val actionModel = rememberScreenModel(
                    tag = "feed-actions-$activeProfileId-${activeFeed.id}-${activePreset.hashCode()}",
                ) {
                    CatalogScreenModel(
                        sourceId = activeSource.id,
                        listingQuery = activePreset.toListing().requestQuery,
                        initialFilterSnapshot = activePreset.filters,
                        initialPresetId = activePreset.id,
                        browseFeedService = Injekt.get<eu.kanade.domain.source.service.BrowseFeedService>()
                            .forProfile(activeProfileId),
                    )
                }
                val actionState by actionModel.state.collectAsState()
                val timelineModel = rememberScreenModel(
                    tag = "feed-timeline-$activeProfileId-${activeFeed.id}-${activePreset.hashCode()}",
                ) {
                    CatalogChronologicalFeedScreenModel(
                        profileId = activeProfileId,
                        feedId = activeFeed.id,
                        sourceId = activeSource.id,
                        listingQuery = activePreset.toListing().requestQuery,
                        initialFilterSnapshot = activePreset.filters,
                        chronological = activePreset.chronological,
                    )
                }
                val timelineState by timelineModel.state.collectAsState()
                val immersiveModel = rememberScreenModel(
                    tag = "feed-immersive-$activeProfileId-${activeFeed.id}",
                ) {
                    EntryImmersiveScreenModel()
                }
                val catalogueFeature = remember { Injekt.get<EntryCatalogueFeature>() }
                val catalogSource = remember(activeSource.id) {
                    (catalogueFeature.source(activeSource.id) as? EntryCatalogueSourceResolution.Available)?.source
                }
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
                            if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) 180.dp else 128.dp,
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
                if (actionState.dialog == CatalogScreenModel.Dialog.Filter) {
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
                        PullRefresh(
                            refreshing = timelineState.isRefreshing,
                            enabled = true,
                            onRefresh = { timelineModel.refresh(manual = true) },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            CatalogFeedBrowseContent(
                                source = catalogSource,
                                screenModel = timelineModel,
                                columns = columns,
                                displayMode = activeDisplayMode,
                                snackbarHostState = snackbarHostState,
                                contentPadding = PaddingValues(
                                    bottom = contentPadding.calculateBottomPadding(),
                                ),
                                onWebViewClick = {
                                    val source = catalogSource
                                    val home = source?.let { Injekt.get<EntrySourceHomeFeature>().resolve(it.id) }
                                        as? EntrySourceHomeResolution.Available
                                    if (source != null && home != null) {
                                        navigator.push(
                                            WebViewScreen(
                                                url = home.url,
                                                initialTitle = source.name,
                                                sourceId = source.id,
                                            ),
                                        )
                                    }
                                },
                                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
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

                if (showFeedPicker && feedViewMode == FeedViewMode.Immersive) {
                    FeedPickerSheet(
                        feeds = enabledFeeds,
                        selectedFeedId = activeFeed.id,
                        sourceFor = screenModel::sourceFor,
                        presetFor = screenModel::presetFor,
                        canJumpToNewest = timelineState.itemRefs.isNotEmpty() &&
                            timelineModel.savedAnchorSnapshot().resolvedItem() != timelineState.itemRefs.firstOrNull(),
                        onSelect = { feedId ->
                            showFeedPicker = false
                            screenModel.selectFeed(feedId)
                        },
                        onRefresh = {
                            showFeedPicker = false
                            timelineModel.refresh(manual = true)
                        },
                        onJumpToNewest = {
                            showFeedPicker = false
                            jumpToNewestRequest++
                        },
                        onAddFeed = {
                            showFeedPicker = false
                            screenModel.showCreateDialog()
                        },
                        onManageFeeds = {
                            showFeedPicker = false
                            screenModel.showManageDialog()
                        },
                        onDismissRequest = { showFeedPicker = false },
                    )
                }

                FeedCatalogActionDialogs(
                    dialog = actionState.dialog,
                    screenModel = actionModel,
                    navigator = navigator,
                )
            }

            if (feedViewMode == FeedViewMode.Regular) {
                FeedNavigationBar(
                    feeds = enabledFeeds,
                    selectedFeed = activeFeed,
                    selectedDisplayMode = activeDisplayMode,
                    sourceFor = screenModel::sourceFor,
                    presetFor = screenModel::presetFor,
                    canGoPrevious = hasPreviousFeed,
                    canGoNext = hasNextFeed,
                    immersiveAvailable = immersiveAvailable,
                    onFeedViewModeChange = onFeedViewModeChange,
                    onDisplayModeChange = { screenModel.updateFeedDisplayMode(activeFeed.id, it) },
                    onPreviousClick = {
                        enabledFeeds.getOrNull(activeIndex - 1)?.let { screenModel.selectFeed(it.id) }
                    },
                    onNextClick = {
                        enabledFeeds.getOrNull(activeIndex + 1)?.let { screenModel.selectFeed(it.id) }
                    },
                    onFeedSelect = screenModel::selectFeed,
                    onRenameFeed = { renamingFeedId = activeFeed.id },
                    onAddFeed = screenModel::showCreateDialog,
                    onManageFeeds = screenModel::showManageDialog,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    when (state.dialog) {
        FeedsScreenModel.Dialog.AddFeed -> {
            AddFeedSheet(
                sources = state.listedSources,
                feeds = state.validFeeds,
                presetsFor = screenModel::presetsFor,
                onSelectPreset = { source, preset -> screenModel.createFeed(source.id, preset.id) },
                onCreateFilteredFeed = { source ->
                    screenModel.closeDialog()
                    navigator.push(CatalogScreen(source.id, listingQuery = null, openFilters = true))
                },
                onDismissRequest = screenModel::closeDialog,
            )
        }
        FeedsScreenModel.Dialog.ManageFeeds -> {
            ManageFeedsSheet(
                feeds = state.validFeeds,
                sourceFor = screenModel::sourceFor,
                presetFor = screenModel::presetFor,
                onReorder = screenModel::reorderFeed,
                onToggle = screenModel::toggleFeed,
                onRename = { renamingFeedId = it.id },
                onRemove = screenModel::removeFeed,
                onRestore = screenModel::restoreFeed,
                onAddFeed = screenModel::showCreateDialog,
                onDismissRequest = screenModel::closeDialog,
            )
        }
        null -> Unit
    }

    val renamingFeed = renamingFeedId?.let { feedId -> state.feeds.firstOrNull { it.id == feedId } }
    val renamingPreset = renamingFeed?.let(screenModel::presetFor)
    if (renamingFeed != null && renamingPreset != null) {
        RenameFeedDialog(
            currentTitle = renamingFeed.title,
            presetName = renamingPreset.displayName(),
            onDismissRequest = { renamingFeedId = null },
            onConfirm = { title ->
                renamingFeedId = null
                screenModel.renameFeed(renamingFeed.id, title)
            },
        )
    }
}

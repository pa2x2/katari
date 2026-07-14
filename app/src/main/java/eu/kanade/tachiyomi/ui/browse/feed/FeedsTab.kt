package eu.kanade.tachiyomi.ui.browse.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.source.model.SourceFeed
import eu.kanade.domain.source.model.SourceFeedContentMode
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.model.toListing
import eu.kanade.presentation.browse.components.BaseSourceItem
import eu.kanade.presentation.browse.components.BrowseEntryPreviewSheet
import eu.kanade.presentation.browse.components.BrowseLibraryActionDialog
import eu.kanade.presentation.browse.components.BrowseMergeEditorDialog
import eu.kanade.presentation.browse.components.DuplicateDetectionLoadingDialog
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.sourceItemOrientation
import eu.kanade.tachiyomi.source.toCatalogSource
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenModel
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveScreenModel
import eu.kanade.tachiyomi.ui.browse.immersive.ImmersiveSystemBarsEffect
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.feature.profiles.core.ProfileManager
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
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
    var feedViewMode by remember { mutableStateOf(FeedViewMode.Regular) }

    return TabContent(
        titleRes = MR.strings.browse_feeds,
        chromeVisible = { feedViewMode == FeedViewMode.Regular },
        tabLabel = if (singleEnabledFeedSource != null && singleEnabledFeedPreset != null) {
            {
                SingleFeedTabLabel(
                    source = singleEnabledFeedSource,
                    preset = singleEnabledFeedPreset,
                )
            }
        } else {
            null
        },
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_add),
                icon = Icons.Outlined.Add,
                onClick = screenModel::showCreateDialog,
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.browse_manage_feeds),
                onClick = screenModel::showManageDialog,
            ),
        ),
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
    source: Source,
    preset: SourceFeedPreset,
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
        Text(
            text = preset.name,
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
    val supportsImmersiveFeed = activeSource?.supportsImmersiveFeed == true
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    ImmersiveSystemBarsEffect(enabled = feedViewMode == FeedViewMode.Immersive)

    LaunchedEffect(activeFeed?.id, supportsImmersiveFeed) {
        if (!supportsImmersiveFeed && feedViewMode == FeedViewMode.Immersive) {
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
            message = stringResource(MR.strings.browse_feeds_empty),
            modifier = Modifier.padding(contentPadding),
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
            key(activeProfileId, activeFeed.id) {
                val catalogSourceManager = remember { Injekt.get<SourceManager>() }
                val actionModel = rememberScreenModel(
                    tag = "feed-actions-$activeProfileId-${activeFeed.id}",
                ) {
                    CatalogScreenModel(
                        sourceId = activeSource.id,
                        listingQuery = activePreset.toListing().requestQuery,
                        initialFilterSnapshot = activePreset.filters,
                    )
                }
                val actionState by actionModel.state.collectAsState()
                val timelineModel = rememberScreenModel(
                    tag = "feed-timeline-$activeProfileId-${activeFeed.id}",
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
                val catalogSource = catalogSourceManager.get(activeSource.id)?.toCatalogSource()
                val sourceItemOrientation = catalogSource?.source?.sourceItemOrientation()
                    ?: EntryItemOrientation.VERTICAL
                val columns = remember(activeDisplayMode) {
                    val isLandscape = context.resources.configuration.orientation ==
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
                            feedLabel = activePreset.name,
                            onShowFeedPicker = { showFeedPicker = true },
                            onExitImmersive = { onFeedViewModeChange(FeedViewMode.Regular) },
                            onEntryClick = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                            onLibraryAction = actionModel::confirmBrowseLibraryAction,
                            onZoomStateChange = {},
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
                                    val source = catalogSource?.source as? SourceHomePage
                                    val homeUrl = source?.getHomeUrl()
                                    if (homeUrl != null) {
                                        navigator.push(
                                            WebViewScreen(
                                                url = homeUrl,
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
                                    scope.launchIO {
                                        if (actionModel.onItemLongClick(item)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        screenModel = screenModel,
                        canJumpToNewest = feedViewMode == FeedViewMode.Immersive &&
                            timelineState.itemRefs.isNotEmpty() &&
                            timelineModel.savedAnchorSnapshot().resolvedItem() != timelineState.itemRefs.firstOrNull(),
                        onSelect = { feedId ->
                            showFeedPicker = false
                            screenModel.selectFeed(feedId)
                        },
                        onRefresh = if (feedViewMode == FeedViewMode.Immersive) {
                            {
                                showFeedPicker = false
                                timelineModel.refresh(manual = true)
                            }
                        } else {
                            null
                        },
                        onJumpToNewest = if (feedViewMode == FeedViewMode.Immersive) {
                            {
                                showFeedPicker = false
                                jumpToNewestRequest++
                            }
                        } else {
                            null
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
                    selectedFeedId = activeFeed.id,
                    selectedDisplayMode = activeDisplayMode,
                    screenModel = screenModel,
                    canGoPrevious = hasPreviousFeed,
                    canGoNext = hasNextFeed,
                    feedViewMode = feedViewMode,
                    supportsImmersiveFeed = supportsImmersiveFeed,
                    onFeedViewModeChange = onFeedViewModeChange,
                    onDisplayModeChange = { screenModel.updateFeedDisplayMode(activeFeed.id, it) },
                    onPreviousClick = {
                        enabledFeeds.getOrNull(activeIndex - 1)?.let { screenModel.selectFeed(it.id) }
                    },
                    onNextClick = {
                        enabledFeeds.getOrNull(activeIndex + 1)?.let { screenModel.selectFeed(it.id) }
                    },
                    pickerExpanded = showFeedPicker,
                    onPickerClick = { showFeedPicker = true },
                    onPickerDismiss = { showFeedPicker = false },
                    onFeedSelect = { feedId ->
                        showFeedPicker = false
                        screenModel.selectFeed(feedId)
                    },
                    onAddFeed = {
                        showFeedPicker = false
                        screenModel.showCreateDialog()
                    },
                    onManageFeeds = {
                        showFeedPicker = false
                        screenModel.showManageDialog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    when (val dialog = state.dialog) {
        FeedsScreenModel.Dialog.SelectSource -> {
            FeedSourcePickerDialog(
                sources = state.sources,
                onDismissRequest = screenModel::closeDialog,
                onSelectSource = screenModel::selectSource,
            )
        }
        is FeedsScreenModel.Dialog.SelectPreset -> {
            val source = screenModel.sourceFor(dialog.sourceId)
            if (source != null) {
                FeedPresetPickerDialog(
                    source = source,
                    presets = screenModel.presetsFor(source),
                    onDismissRequest = screenModel::closeDialog,
                    onSelectPreset = { preset -> screenModel.createFeed(source.id, preset.id) },
                )
            }
        }
        FeedsScreenModel.Dialog.ManageFeeds -> {
            ManageFeedsDialog(
                state = state,
                screenModel = screenModel,
                onDismissRequest = screenModel::closeDialog,
            )
        }
        null -> Unit
    }
}

@Composable
private fun Screen.FeedCatalogActionDialogs(
    dialog: CatalogScreenModel.Dialog?,
    screenModel: CatalogScreenModel,
    navigator: Navigator,
) {
    val onDismissRequest = screenModel::dismissDialog
    when (dialog) {
        is CatalogScreenModel.Dialog.EntryPreview -> {
            BrowseEntryPreviewSheet(
                entryId = dialog.entryId,
                onLibraryAction = screenModel::confirmBrowseLibraryAction,
                onMergeAction = screenModel::showMergeTargetPicker,
                onOpenEntry = {
                    onDismissRequest()
                    navigator.push(EntryScreen(it, fromSource = true))
                },
                onDismissRequest = onDismissRequest,
            )
        }
        is CatalogScreenModel.Dialog.LibraryActionChooser -> {
            BrowseLibraryActionDialog(
                mangaTitle = dialog.entry.displayTitle,
                favorite = dialog.entry.favorite,
                onDismissRequest = onDismissRequest,
                onLibraryAction = {
                    onDismissRequest()
                    screenModel.confirmBrowseLibraryAction(dialog.entry)
                },
                onMergeIntoLibrary = { screenModel.showMergeTargetPicker(dialog.entry) },
            )
        }
        CatalogScreenModel.Dialog.CheckingDuplicates -> DuplicateDetectionLoadingDialog()
        is CatalogScreenModel.Dialog.RemoveEntry -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.changeFavorite(dialog.entry) },
                mangaToRemove = dialog.entry,
            )
        }
        is CatalogScreenModel.Dialog.DuplicateEntry -> {
            DuplicateEntryDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.addFavorite(dialog.entry) },
                onOpenEntry = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                onMigrate = {
                    screenModel.showMigrateEntryDialog(current = it, target = dialog.entry)
                },
            )
        }
        is CatalogScreenModel.Dialog.SelectEntryMergeTarget -> {
            MergeTargetPickerDialog(
                title = stringResource(MR.strings.action_merge_into_library),
                query = dialog.query,
                visibleTargets = dialog.visibleTargets,
                onDismissRequest = onDismissRequest,
                onQueryChange = screenModel::updateMergeTargetQuery,
                onSelectTarget = screenModel::openMergeEditor,
            )
        }
        is CatalogScreenModel.Dialog.EditEntryMerge -> {
            BrowseMergeEditorDialog(
                entries = dialog.entries,
                targetId = dialog.targetId,
                targetLocked = dialog.targetLocked,
                removedIds = dialog.removedIds,
                libraryRemovalIds = dialog.libraryRemovalIds,
                confirmEnabled = dialog.enabled,
                onDismissRequest = onDismissRequest,
                onMove = screenModel::moveMergeEntry,
                onSelectTarget = screenModel::setMergeTarget,
                onToggleRemove = screenModel::toggleMergeEntryRemoval,
                onToggleLibraryRemove = screenModel::toggleMergeEntryLibraryRemoval,
                onConfirm = screenModel::confirmBrowseMerge,
            )
        }
        is CatalogScreenModel.Dialog.ChangeEntryCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    screenModel.changeFavorite(dialog.entry)
                    screenModel.moveEntryToCategories(dialog.entry, include)
                },
            )
        }
        is CatalogScreenModel.Dialog.MigrateEntry -> {
            MigrateEntryDialog(
                current = dialog.current,
                target = dialog.target,
                onClickTitle = { navigator.push(EntryScreen(dialog.current.id, fromSource = true)) },
                onDismissRequest = onDismissRequest,
                onComplete = {
                    screenModel.dismissDialog()
                    navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                },
            )
        }
        CatalogScreenModel.Dialog.Filter,
        is CatalogScreenModel.Dialog.SavePreset,
        null,
        -> Unit
    }
}

@Composable
private fun FeedNavigationBar(
    feeds: List<SourceFeed>,
    selectedFeedId: String,
    selectedDisplayMode: LibraryDisplayMode,
    screenModel: FeedsScreenModel,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    feedViewMode: FeedViewMode,
    supportsImmersiveFeed: Boolean,
    onFeedViewModeChange: (FeedViewMode) -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    pickerExpanded: Boolean,
    onPickerClick: () -> Unit,
    onPickerDismiss: () -> Unit,
    onFeedSelect: (String) -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedFeed = feeds.firstOrNull { it.id == selectedFeedId }
    val selectedSource = selectedFeed?.let { screenModel.sourceFor(it.sourceId) }
    val selectedPreset = selectedFeed?.let(screenModel::presetFor)

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = MaterialTheme.padding.small),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.padding.small,
                    end = MaterialTheme.padding.small,
                    top = MaterialTheme.padding.small,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousClick, enabled = canGoPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.transition_previous),
                )
            }
            if (selectedSource != null && selectedPreset != null) {
                FeedSelectorMenu(
                    feeds = feeds,
                    selectedFeedId = selectedFeedId,
                    selectedSource = selectedSource,
                    selectedPreset = selectedPreset,
                    screenModel = screenModel,
                    expanded = pickerExpanded,
                    onExpandedChange = { expanded ->
                        if (expanded) onPickerClick() else onPickerDismiss()
                    },
                    onFeedSelect = onFeedSelect,
                    onAddFeed = onAddFeed,
                    onManageFeeds = onManageFeeds,
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onNextClick, enabled = canGoNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(MR.strings.transition_next),
                )
            }
            FeedViewModeButton(
                viewMode = feedViewMode,
                supportsImmersiveFeed = supportsImmersiveFeed,
                onViewModeChange = onFeedViewModeChange,
            )
            if (feedViewMode == FeedViewMode.Regular) {
                FeedDisplayModeButton(
                    displayMode = selectedDisplayMode,
                    onDisplayModeChange = onDisplayModeChange,
                )
            }
        }
    }
}

@Composable
private fun FeedViewModeButton(
    viewMode: FeedViewMode,
    supportsImmersiveFeed: Boolean,
    onViewModeChange: (FeedViewMode) -> Unit,
) {
    IconButton(
        onClick = {
            onViewModeChange(
                if (viewMode == FeedViewMode.Regular) FeedViewMode.Immersive else FeedViewMode.Regular,
            )
        },
        enabled = supportsImmersiveFeed,
    ) {
        Icon(
            imageVector = Icons.Outlined.Fullscreen,
            contentDescription = stringResource(MR.strings.browse_feed_enter_immersive),
        )
    }
}

internal fun availableFeedViewModes(supportsImmersiveFeed: Boolean): List<FeedViewMode> {
    return if (supportsImmersiveFeed) {
        listOf(FeedViewMode.Regular, FeedViewMode.Immersive)
    } else {
        listOf(FeedViewMode.Regular)
    }
}

@Composable
private fun FeedDisplayModeButton(
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    var selectingDisplayMode by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { selectingDisplayMode = true }) {
            Icon(
                imageVector = if (displayMode == LibraryDisplayMode.List ||
                    displayMode == LibraryDisplayMode.ComfortableList
                ) {
                    Icons.AutoMirrored.Filled.ViewList
                } else {
                    Icons.Filled.ViewModule
                },
                contentDescription = stringResource(MR.strings.action_display_mode),
            )
        }
        FeedDisplayModeDropdown(
            expanded = selectingDisplayMode,
            selectedDisplayMode = displayMode,
            onDismissRequest = { selectingDisplayMode = false },
            onDisplayModeChange = {
                selectingDisplayMode = false
                onDisplayModeChange(it)
            },
        )
    }
}

@Composable
private fun FeedSelectorMenu(
    feeds: List<SourceFeed>,
    selectedFeedId: String,
    selectedSource: Source,
    selectedPreset: SourceFeedPreset,
    screenModel: FeedsScreenModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFeedSelect: (String) -> Unit,
    onAddFeed: () -> Unit,
    onManageFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        FeedSelectorButton(
            source = selectedSource,
            preset = selectedPreset,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
        ) {
            feeds.forEach { feed ->
                val source = screenModel.sourceFor(feed.sourceId) ?: return@forEach
                val preset = screenModel.presetFor(feed) ?: return@forEach
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = preset.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = { onFeedSelect(feed.id) },
                    leadingIcon = {
                        SourceIcon(
                            source = source,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                        )
                    },
                    trailingIcon = if (feed.id == selectedFeedId) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(MR.strings.selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.action_add)) },
                onClick = onAddFeed,
                leadingIcon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(MR.strings.browse_manage_feeds)) },
                onClick = onManageFeeds,
                leadingIcon = { Icon(imageVector = Icons.Outlined.DragHandle, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun FeedSelectorButton(
    source: Source,
    preset: SourceFeedPreset,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIcon(
                source = source,
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun FeedSourcePickerDialog(
    sources: List<Source>,
    onDismissRequest: () -> Unit,
    onSelectSource: (Source) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.padding(top = MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.action_add),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
            )
            ScrollbarLazyColumn(
                contentPadding = topSmallPaddingValues,
            ) {
                items(
                    items = sources,
                    key = { it.id },
                ) { source ->
                    BaseSourceItem(
                        source = source,
                        modifier = Modifier
                            .animateItemFastScroll()
                            .padding(vertical = MaterialTheme.padding.small),
                        onClickItem = { onSelectSource(source) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPresetPickerDialog(
    source: Source,
    presets: List<SourceFeedPreset>,
    onDismissRequest: () -> Unit,
    onSelectPreset: (SourceFeedPreset) -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(vertical = MaterialTheme.padding.small)) {
            Text(
                text = source.name,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.small,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            presets.forEach { preset ->
                FeedPresetItem(
                    source = source,
                    preset = preset,
                    onClick = { onSelectPreset(preset) },
                )
            }
        }
    }
}

@Composable
private fun FeedPresetItem(
    source: Source,
    preset: SourceFeedPreset,
    onClick: () -> Unit,
) {
    BaseSourceItem(
        source = source,
        showLanguageInContent = false,
        onClickItem = onClick,
        content = { _, _ ->
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = preset.name,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                )
            }
        },
    )
}

@Composable
private fun ManageFeedsDialog(
    state: FeedsScreenModel.State,
    screenModel: FeedsScreenModel,
    onDismissRequest: () -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        listState,
        PaddingValues(vertical = MaterialTheme.padding.small),
    ) { from, to ->
        screenModel.reorderFeed(from.key as String, to.key as String)
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.browse_manage_feeds)) },
        text = {
            ScrollbarLazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = MaterialTheme.padding.small),
            ) {
                items(
                    items = state.validFeeds,
                    key = { it.id },
                ) { feed ->
                    val source = screenModel.sourceFor(feed.sourceId)
                    val preset = screenModel.presetFor(feed)
                    ReorderableItem(
                        state = reorderableState,
                        key = feed.id,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.padding.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = null,
                                modifier = Modifier.draggableHandle(),
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                            if (source != null && preset != null) {
                                SourceIcon(
                                    source = source,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(MaterialTheme.shapes.extraSmall),
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                                Text(
                                    text = preset.name,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = feed.enabled,
                                onCheckedChange = { screenModel.toggleFeed(feed.id, it) },
                            )
                            IconButton(onClick = { screenModel.removeFeed(feed.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(MR.strings.action_delete),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
private fun FeedDisplayModeDropdown(
    expanded: Boolean,
    selectedDisplayMode: LibraryDisplayMode,
    onDismissRequest: () -> Unit,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        LibraryDisplayMode.values.forEach { mode ->
            RadioMenuItem(
                text = { Text(text = stringResource(mode.titleRes())) },
                isChecked = selectedDisplayMode == mode,
            ) { onDisplayModeChange(mode) }
        }
    }
}

private fun LibraryDisplayMode.titleRes(): StringResource {
    return when (this) {
        LibraryDisplayMode.ComfortableGrid -> MR.strings.action_display_comfortable_grid
        LibraryDisplayMode.CompactGrid -> MR.strings.action_display_grid
        LibraryDisplayMode.CoverOnlyGrid -> MR.strings.action_display_grid
        LibraryDisplayMode.List -> MR.strings.action_display_list
        LibraryDisplayMode.ComfortableList -> MR.strings.action_display_comfortable_list
    }
}

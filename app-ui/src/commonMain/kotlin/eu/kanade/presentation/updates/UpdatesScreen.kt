package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CalendarMonth
import app.katari.icons.materialsymbols.rounded.Deselect
import app.katari.icons.materialsymbols.rounded.FilterList
import app.katari.icons.materialsymbols.rounded.SelectAll
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarExtension
import app.katari.presentation.kit.navigation.AppBarScrollBehavior
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.refresh.PullRefresh
import app.katari.presentation.kit.refresh.refreshAppBarAction
import app.katari.presentation.kit.selection.ItemSelection
import app.katari.presentation.kit.selection.SelectableItems
import app.katari.presentation.kit.selection.SelectionActions
import app.katari.presentation.kit.theme.ContentTone
import app.katari.profiles.ui.switcher.ActiveProfileButton
import app.katari.sync.ui.download.DownloadElsewhereRequest
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import eu.kanade.presentation.entry.components.chapterSelectionActions
import eu.kanade.presentation.library.update.rememberAwaitingUpdateState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import tachiyomi.domain.library.update.model.LibraryUpdateState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun <T> UpdatesScreen(
    state: UpdatesScreenState<T>,
    snackbarHostState: SnackbarHostState,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    libraryUpdate: LibraryUpdateState?,
    onUpdateLibrary: (suspend () -> Boolean)?,
    onCalendarClicked: (() -> Unit)?,
    onFilterClicked: (() -> Unit)?,
    hasActiveFilters: Boolean,
    syncAction: AppBar.Action?,
    filterSummary: (@Composable () -> Unit)? = null,
    loadingContent: @Composable (Modifier) -> Unit = { LoadingScreen(it) },
    emptyContent: @Composable (Modifier) -> Unit = {
        EmptyScreen(
            stringRes = MR.strings.information_no_recent,
            modifier = it,
        )
    },
    content: LazyListScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selectionActions = state.bottomBarConfig?.let { updatesSelectionActions(it) }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.selectionMode,
        onBackCompleted = { onSelectAll(false) },
    )

    Scaffold(
        topBar = { scrollBehavior ->
            Column {
                UpdatesAppBar(
                    onCalendarClicked = onCalendarClicked,
                    onUpdateLibrary = onUpdateLibrary?.let { update -> { scope.launch { update() } } },
                    onFilterClicked = onFilterClicked,
                    hasFilters = hasActiveFilters,
                    syncAction = syncAction,
                    actionModeCounter = state.selectedCount,
                    onSelectAll = { onSelectAll(true) },
                    onInvertSelection = onInvertSelection,
                    onCancelActionMode = { onSelectAll(false) },
                    scrollBehavior = scrollBehavior,
                )
                if (filterSummary != null) {
                    AppBarExtension(actionMode = state.selectionMode) { filterSummary() }
                }
            }
        },
        bottomBar = {
            if (state.bottomBarConfig != null && selectionActions != null) {
                EntryBottomActionMenu(
                    visible = state.bottomBarConfig.visible,
                    actions = selectionActions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { contentPadding ->
        when {
            state.isLoading -> loadingContent(Modifier.padding(contentPadding))
            else -> {
                var awaitingUpdate by rememberAwaitingUpdateState(libraryUpdate)

                PullRefresh(
                    modifier = Modifier.fillMaxSize(),
                    refreshing = awaitingUpdate,
                    onRefresh = {
                        val update = onUpdateLibrary ?: return@PullRefresh
                        scope.launch {
                            if (update()) awaitingUpdate = true
                        }
                    },
                    enabled = !state.selectionMode && onUpdateLibrary != null,
                    indicatorPadding = contentPadding,
                ) {
                    when {
                        state.isEmpty -> emptyContent(Modifier.padding(contentPadding))
                        else -> SelectableItems(
                            selection = ItemSelection(
                                active = state.selectionMode,
                                onSelectAll = { onSelectAll(true) },
                                onClear = { onSelectAll(false) },
                            ),
                            actions = selectionActions,
                        ) {
                            RowList(
                                fastScroll = true,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                                content = content,
                            )
                        }
                    }
                }
            }
        }
    }
}

data class UpdatesScreenState<T>(
    val isLoading: Boolean,
    val isEmpty: Boolean,
    val selectionMode: Boolean,
    val selectedCount: Int,
    val bottomBarConfig: UpdatesBottomBarConfig? = null,
)

data class UpdatesBottomBarConfig(
    val visible: Boolean,
    val onBookmarkClicked: (() -> Unit)? = null,
    val onRemoveBookmarkClicked: (() -> Unit)? = null,
    val onMarkAsReadClicked: (() -> Unit)? = null,
    val onMarkAsUnreadClicked: (() -> Unit)? = null,
    val markAsReadLabel: StringResource = MR.strings.action_mark_as_read,
    val markAsUnreadLabel: StringResource = MR.strings.action_mark_as_unread,
    val onDownloadClicked: (() -> Unit)? = null,
    val onDeleteClicked: (() -> Unit)? = null,
    val downloadElsewhere: DownloadElsewhereRequest? = null,
    val onDownloadElsewhereOpened: () -> Unit = {},
)

@Composable
private fun UpdatesAppBar(
    onCalendarClicked: (() -> Unit)?,
    onUpdateLibrary: (() -> Unit)?,
    onFilterClicked: (() -> Unit)?,
    hasFilters: Boolean,
    syncAction: AppBar.Action?,
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    scrollBehavior: AppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        title = stringResource(MR.strings.label_recent_updates),
        actions = {
            val actions = buildList {
                syncAction?.let(::add)
                if (onFilterClicked != null) {
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_filter),
                            icon = MaterialSymbols.Rounded.FilterList,
                            tone = if (hasFilters) ContentTone.Active else ContentTone.Inherit,
                            onClick = onFilterClicked,
                        ),
                    )
                }
                if (onCalendarClicked != null) {
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_view_upcoming),
                            icon = MaterialSymbols.Rounded.CalendarMonth,
                            onClick = onCalendarClicked,
                        ),
                    )
                }
                if (onUpdateLibrary != null) {
                    add(
                        refreshAppBarAction(
                            onClick = onUpdateLibrary,
                            title = stringResource(MR.strings.action_update_library),
                        ),
                    )
                }
            }

            AppBarActions(actions = actions.toPersistentList())
            ActiveProfileButton()
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.Deselect,
                        onClick = onInvertSelection,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun updatesSelectionActions(config: UpdatesBottomBarConfig): SelectionActions = chapterSelectionActions(
    onBookmarkClicked = config.onBookmarkClicked,
    onRemoveBookmarkClicked = config.onRemoveBookmarkClicked,
    onMarkAsReadClicked = config.onMarkAsReadClicked,
    onMarkAsUnreadClicked = config.onMarkAsUnreadClicked,
    markAsReadLabel = config.markAsReadLabel,
    markAsUnreadLabel = config.markAsUnreadLabel,
    onDownloadClicked = config.onDownloadClicked,
    onDeleteClicked = config.onDeleteClicked,
    downloadElsewhere = config.downloadElsewhere,
    onDownloadElsewhereOpened = config.onDownloadElsewhereOpened,
)

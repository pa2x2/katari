package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import eu.kanade.presentation.entry.components.chapterSelectionActions
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.feature.profiles.ui.switcher.ActiveProfileButton
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarExtension
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.refresh.PullRefresh
import mihon.presentation.kit.refresh.refreshAppBarAction
import mihon.presentation.kit.selection.ItemSelection
import mihon.presentation.kit.selection.SelectableItems
import mihon.presentation.kit.selection.SelectionActions
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun <T> UpdatesScreen(
    state: UpdatesScreenState<T>,
    snackbarHostState: SnackbarHostState,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: (suspend () -> Boolean)?,
    onCalendarClicked: (() -> Unit)?,
    onFilterClicked: (() -> Unit)?,
    hasActiveFilters: Boolean,
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
                    actionModeCounter = state.selectedCount,
                    onSelectAll = { onSelectAll(true) },
                    onInvertSelection = onInvertSelection,
                    onCancelActionMode = { onSelectAll(false) },
                    scrollBehavior = scrollBehavior,
                )
                if (filterSummary != null) {
                    // The feed scrolls under the bar, so it takes the app bar's colour, raised in action mode.
                    Surface(
                        container = if (state.selectionMode) SurfaceContainer.Default else SurfaceContainer.Surface,
                    ) {
                        AppBarExtension { filterSummary() }
                    }
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
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    modifier = Modifier.fillMaxSize(),
                    refreshing = isRefreshing,
                    onRefresh = {
                        val update = onUpdateLibrary ?: return@PullRefresh
                        scope.launch {
                            val started = update()
                            if (!started) return@launch
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
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
)

@Composable
private fun UpdatesAppBar(
    onCalendarClicked: (() -> Unit)?,
    onUpdateLibrary: (() -> Unit)?,
    onFilterClicked: (() -> Unit)?,
    hasFilters: Boolean,
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
)

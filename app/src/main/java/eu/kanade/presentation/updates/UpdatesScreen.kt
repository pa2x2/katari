package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
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
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.feature.profiles.ui.switcher.ActiveProfileButton
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.progress.PullRefresh
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
    onUpdateLibrary: suspend () -> Boolean,
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

    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            Column {
                UpdatesAppBar(
                    onCalendarClicked = onCalendarClicked,
                    onUpdateLibrary = { scope.launch { onUpdateLibrary() } },
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
                        filterSummary()
                    }
                }
            }
        },
        bottomBar = {
            state.bottomBarConfig?.let { bottomBarConfig ->
                UpdatesBottomBar(config = bottomBarConfig)
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
                        scope.launch {
                            val started = onUpdateLibrary()
                            if (!started) return@launch
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    when {
                        state.isEmpty -> emptyContent(Modifier.padding(contentPadding))
                        else -> RowList(
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
    onUpdateLibrary: () -> Unit,
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
                add(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_update_library),
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = onUpdateLibrary,
                    ),
                )
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
private fun UpdatesBottomBar(
    config: UpdatesBottomBarConfig,
) {
    EntryBottomActionMenu(
        visible = config.visible,
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = config.onBookmarkClicked,
        onRemoveBookmarkClicked = config.onRemoveBookmarkClicked,
        onMarkAsReadClicked = config.onMarkAsReadClicked,
        onMarkAsUnreadClicked = config.onMarkAsUnreadClicked,
        markAsReadLabel = config.markAsReadLabel,
        markAsUnreadLabel = config.markAsUnreadLabel,
        onDownloadClicked = config.onDownloadClicked,
        onDeleteClicked = config.onDeleteClicked,
    )
}

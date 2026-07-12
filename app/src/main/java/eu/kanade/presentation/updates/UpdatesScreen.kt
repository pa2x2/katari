package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.active
import kotlin.time.Duration.Companion.seconds

@Composable
fun <T> UpdatesScreen(
    state: UpdatesScreenState<T>,
    snackbarHostState: SnackbarHostState,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onCalendarClicked: (() -> Unit)?,
    onFilterClicked: (() -> Unit)?,
    hasActiveFilters: Boolean,
    loadingContent: @Composable (Modifier) -> Unit = { LoadingScreen(it) },
    emptyContent: @Composable (Modifier) -> Unit = {
        EmptyScreen(
            stringRes = MR.strings.information_no_recent,
            modifier = it,
        )
    },
    content: LazyListScope.() -> Unit,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            UpdatesAppBar(
                onCalendarClicked = onCalendarClicked,
                onUpdateLibrary = { onUpdateLibrary() },
                onFilterClicked = onFilterClicked,
                hasFilters = hasActiveFilters,
                actionModeCounter = state.selectedCount,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = onInvertSelection,
                onCancelActionMode = { onSelectAll(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            state.bottomBarConfig?.let { bottomBarConfig ->
                UpdatesBottomBar(config = bottomBarConfig)
            }
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> loadingContent(Modifier.padding(contentPadding))
            else -> {
                val scope = rememberCoroutineScope()
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    modifier = Modifier.fillMaxSize(),
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
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
                        else -> FastScrollLazyColumn(
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
    scrollBehavior: TopAppBarScrollBehavior,
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
                            icon = Icons.Outlined.FilterList,
                            iconTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current,
                            onClick = onFilterClicked,
                        ),
                    )
                }
                if (onCalendarClicked != null) {
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_view_upcoming),
                            icon = Icons.Outlined.CalendarMonth,
                            onClick = onCalendarClicked,
                        ),
                    )
                }
                add(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_update_library),
                        icon = Icons.Outlined.Refresh,
                        onClick = onUpdateLibrary,
                    ),
                )
            }

            AppBarActions(actions = actions.toPersistentList())
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = Icons.Outlined.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = Icons.Outlined.FlipToBack,
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

package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveContent
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveItemKey
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveScreenModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
internal fun CatalogImmersiveContent(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    immersiveModel: EntryImmersiveScreenModel,
    sourceName: String,
    snackbarHostState: SnackbarHostState,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    positionState: EntryImmersivePositionState,
    modifier: Modifier = Modifier,
) {
    val refreshError = catalogList.loadState.refresh as? LoadState.Error
    val appendError = catalogList.loadState.append as? LoadState.Error
    val error = refreshError ?: appendError
    val retryLabel = stringResource(MR.strings.action_retry)
    val unknownError = stringResource(MR.strings.unknown_error)

    LaunchedEffect(error) {
        if (catalogList.itemCount == 0 || error == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = error.error.message ?: unknownError,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) catalogList.retry()
    }

    if (catalogList.itemCount == 0 && catalogList.loadState.refresh is LoadState.Loading) {
        LoadingScreen(modifier)
        return
    }

    if (catalogList.itemCount == 0) {
        EmptyScreen(
            modifier = modifier,
            message = error?.error?.message ?: stringResource(MR.strings.no_results_found),
            actions = persistentListOf(
                EmptyScreenAction(
                    stringRes = MR.strings.action_retry,
                    icon = Icons.Outlined.Refresh,
                    onClick = catalogList::refresh,
                ),
            ),
        )
        return
    }

    EntryImmersiveContent(
        itemCount = catalogList.itemCount,
        itemIdentity = { page ->
            val entry = (catalogList.peek(page)?.value as? CatalogListItem.EntryItem)?.entry
            entry?.let { EntryImmersiveItemKey(id = it.id, type = it.type) }
        },
        itemContent = { page -> catalogEntry(catalogList, page) },
        immersiveModel = immersiveModel,
        contextLabel = sourceName,
        onContextClick = null,
        onExitImmersive = onExitImmersive,
        onEntryClick = onEntryClick,
        onLibraryAction = onLibraryAction,
        onZoomStateChange = {},
        positionState = positionState,
        refreshing = catalogList.loadState.refresh is LoadState.Loading,
        onRefresh = catalogList::refresh,
        modifier = modifier,
    )
}

@Composable
private fun catalogEntry(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem>>,
    index: Int,
): Entry? {
    val itemFlow = catalogList[index] ?: return null
    val item by itemFlow.collectAsState()
    return (item as? CatalogListItem.EntryItem)?.entry
}

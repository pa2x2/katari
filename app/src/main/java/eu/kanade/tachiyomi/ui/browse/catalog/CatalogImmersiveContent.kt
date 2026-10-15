package eu.kanade.tachiyomi.ui.browse.catalog

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
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.EmptyScreenAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarDuration
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.feedback.SnackbarResult
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun CatalogImmersiveContent(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    immersiveModel: EntryImmersiveViewModel,
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
                    icon = MaterialSymbols.Rounded.Refresh,
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
        onPagingBlockedChange = {},
        positionState = positionState,
        refreshing = catalogList.loadState.refresh is LoadState.Loading,
        onRefresh = catalogList::refresh,
        modifier = modifier,
    )
}

@Composable
private fun catalogEntry(
    catalogList: LazyPagingItems<StateFlow<CatalogListItem?>>,
    index: Int,
): Entry? {
    val itemFlow = catalogList[index] ?: return null
    val item by itemFlow.collectAsState()
    return (item as? CatalogListItem.EntryItem)?.entry
}

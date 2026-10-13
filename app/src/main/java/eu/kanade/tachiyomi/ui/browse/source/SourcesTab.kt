package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.interactor.SourceListListing
import eu.kanade.domain.source.model.CATALOGUE_LATEST_QUERY
import eu.kanade.domain.source.model.CATALOGUE_POPULAR_QUERY
import eu.kanade.presentation.browse.SourceOptionsDialog
import eu.kanade.presentation.browse.SourcesFilterSheet
import eu.kanade.presentation.browse.SourcesScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.TravelExplore
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun Screen.sourcesTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = viewModel { SourcesViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    return TabContent(
        titleRes = MR.strings.label_sources,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_global_search),
                icon = MaterialSymbols.Rounded.TravelExplore,
                onClick = { navigator.push(GlobalSearchScreen()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = MaterialSymbols.Rounded.FilterList,
                badgeCount = 1.takeIf { state.contentTypeFilter.isActive },
                onClick = { showFilters = true },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            SourcesScreen(
                state = state.listState,
                contentTypeFilter = state.contentTypeFilter,
                contentPadding = contentPadding,
                onClickItem = { source, listing ->
                    navigator.push(CatalogScreen(source.id, listingQuery(listing)))
                },
                onClickPin = viewModel::togglePin,
                onLongClickItem = viewModel::showSourceDialog,
            )

            if (showFilters) {
                SourcesFilterSheet(
                    state = state.filter,
                    onDismissRequest = { showFilters = false },
                    onClickLanguage = viewModel::toggleLanguage,
                    onClickSource = viewModel::toggleSource,
                    onShowAllContentTypes = viewModel::showAllContentTypes,
                    onToggleContentType = viewModel::toggleContentType,
                    onToggleUnspecifiedContentType = viewModel::toggleUnspecifiedContentType,
                )
            }

            state.dialog?.let { dialog ->
                val source = dialog.source
                SourceOptionsDialog(
                    source = source,
                    onClickPin = {
                        viewModel.togglePin(source)
                        viewModel.closeDialog()
                    },
                    onClickDisable = {
                        viewModel.toggleSource(source)
                        viewModel.closeDialog()
                    },
                    onDismiss = viewModel::closeDialog,
                )
            }

            val internalErrString = stringResource(MR.strings.internal_error)
            LaunchedEffect(Unit) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        SourcesViewModel.Event.FailedFetchingSources -> {
                            launch { snackbarHostState.showSnackbar(internalErrString) }
                        }
                    }
                }
            }
        },
    )
}

private fun listingQuery(listing: SourceListListing): String? {
    return when (listing) {
        SourceListListing.Popular -> CATALOGUE_POPULAR_QUERY
        SourceListListing.Latest -> CATALOGUE_LATEST_QUERY
    }
}

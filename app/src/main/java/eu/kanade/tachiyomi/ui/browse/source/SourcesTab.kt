package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.rememberScreenModel
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
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun Screen.sourcesTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { SourcesScreenModel() }
    val state by screenModel.state.collectAsState()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    return TabContent(
        titleRes = MR.strings.label_sources,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_global_search),
                icon = Icons.Outlined.TravelExplore,
                onClick = { navigator.push(GlobalSearchScreen()) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_filter),
                icon = Icons.Outlined.FilterList,
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
                onClickPin = screenModel::togglePin,
                onLongClickItem = screenModel::showSourceDialog,
            )

            if (showFilters) {
                SourcesFilterSheet(
                    state = state.filter,
                    onDismissRequest = { showFilters = false },
                    onClickLanguage = screenModel::toggleLanguage,
                    onClickSource = screenModel::toggleSource,
                    onShowAllContentTypes = screenModel::showAllContentTypes,
                    onToggleContentType = screenModel::toggleContentType,
                    onToggleUnspecifiedContentType = screenModel::toggleUnspecifiedContentType,
                )
            }

            state.dialog?.let { dialog ->
                val source = dialog.source
                SourceOptionsDialog(
                    source = source,
                    onClickPin = {
                        screenModel.togglePin(source)
                        screenModel.closeDialog()
                    },
                    onClickDisable = {
                        screenModel.toggleSource(source)
                        screenModel.closeDialog()
                    },
                    onDismiss = screenModel::closeDialog,
                )
            }

            val internalErrString = stringResource(MR.strings.internal_error)
            LaunchedEffect(Unit) {
                screenModel.events.collectLatest { event ->
                    when (event) {
                        SourcesScreenModel.Event.FailedFetchingSources -> {
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

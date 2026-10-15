package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifCatalogSourcesLoaded
import eu.kanade.presentation.browse.GlobalSearchScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import kotlinx.coroutines.launch
import mihon.presentation.kit.feedback.LoadingScreen

class GlobalSearchScreen(
    val searchQuery: String = "",
    private val extensionFilter: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifCatalogSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val viewModel = viewModel {
            GlobalSearchViewModel(
                initialQuery = searchQuery,
                initialExtensionFilter = extensionFilter,
            )
        }
        val state by viewModel.state.collectAsState()
        var showSingleLoadingScreen by remember {
            mutableStateOf(searchQuery.isNotEmpty() && !extensionFilter.isNullOrEmpty() && state.total == 1)
        }

        if (showSingleLoadingScreen) {
            LoadingScreen()

            LaunchedEffect(state.items) {
                when (val result = state.items.values.singleOrNull()) {
                    GlobalSearchItemResult.Loading -> return@LaunchedEffect
                    is GlobalSearchItemResult.Success -> {
                        val item = result.result.singleOrNull()
                        if (item != null) {
                            navigator.replace(EntryScreen(item.id, fromSource = true))
                        } else {
                            showSingleLoadingScreen = false
                        }
                    }
                    else -> showSingleLoadingScreen = false
                }
            }
        } else {
            GlobalSearchScreen(
                state = state,
                navigateUp = navigator::pop,
                onChangeSearchQuery = viewModel::updateSearchQuery,
                onSearch = { viewModel.search() },
                getItem = { viewModel.getItem(it) },
                onChangeSearchFilter = viewModel::setSourceFilter,
                onToggleResults = viewModel::toggleFilterResults,
                onClickSource = {
                    navigator.push(CatalogScreen(it.id, state.searchQuery))
                },
                onClickItem = { item ->
                    navigator.push(EntryScreen(item.id, fromSource = true))
                },
                onLongClickItem = { item ->
                    navigator.push(EntryScreen(item.id, fromSource = true))
                },
            )
        }
    }
}

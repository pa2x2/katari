package eu.kanade.tachiyomi.ui.browse

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.presentation.kit.layout.ScreenFrame
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.feed.feedsTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.home.LocalHomeContentReadiness
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 3u,
            title = stringResource(MR.strings.browse),
        )

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    /** Browse pages that other screens can open directly. */
    enum class Page { Sources, Extensions }

    private val switchToPageChannel = Channel<Page>(1, BufferOverflow.DROP_OLDEST)

    fun showPage(page: Page) {
        switchToPageChannel.trySend(page)
    }

    @Composable
    override fun Content() {
        val graph = LocalAppUiGraph.current
        val customPreferences = remember { graph.customPreferences }
        val feedsEnabled by customPreferences.enableFeeds.collectAsState()

        // Hoisted for extensions tab's search bar
        val extensionsViewModel = metroViewModel<ExtensionsViewModel>()
        val extensionsSearchQuery by extensionsViewModel.searchQuery.collectAsStateWithLifecycle()

        val tabs = if (feedsEnabled) {
            persistentListOf(
                feedsTab(),
                sourcesTab(),
                extensionsTab(extensionsViewModel),
                migrateSourceTab(),
            )
        } else {
            persistentListOf(
                sourcesTab(),
                extensionsTab(extensionsViewModel),
                migrateSourceTab(),
            )
        }

        val sourcesTabIndex = if (feedsEnabled) 1 else 0
        val extensionsTabIndex = sourcesTabIndex + 1

        val state = rememberPagerState { tabs.size }

        LaunchedEffect(tabs.size) {
            val maxIndex = tabs.lastIndex.coerceAtLeast(0)
            if (state.currentPage > maxIndex) {
                state.scrollToPage(maxIndex)
            }
        }

        TabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs,
            state = state,
            searchQuery = extensionsSearchQuery,
            onChangeSearchQuery = extensionsViewModel::search,
            // Feeds are cover grids
            frame = ScreenFrame.Wide,
        )
        LaunchedEffect(state, sourcesTabIndex) {
            switchToPageChannel.receiveAsFlow()
                .collectLatest { page ->
                    val index = when (page) {
                        Page.Sources -> sourcesTabIndex
                        Page.Extensions -> extensionsTabIndex
                    }
                    state.scrollToPage(index)
                }
        }

        val contentReadiness = LocalHomeContentReadiness.current
        LaunchedEffect(Unit) {
            contentReadiness.onContentReady()
        }
    }
}

package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
import mihon.feature.profiles.ui.switcher.ActiveProfileButton
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.SearchToolbar
import mihon.presentation.kit.navigation.Tab
import mihon.presentation.kit.navigation.TabRow
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TabbedScreen(
    titleRes: StringResource,
    tabs: List<TabContent>,
    state: PagerState = rememberPagerState { tabs.size },
    searchQuery: String? = null,
    onChangeSearchQuery: (String?) -> Unit = {},
    navigateUp: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scope = rememberCoroutineScope()
    val tab = tabs[state.currentPage]
    val chromeVisible = tab.chromeVisible()

    Scaffold(
        topBar = { scrollBehavior ->
            if (chromeVisible) {
                val searchEnabled = tab.searchEnabled

                SearchToolbar(
                    titleContent = { AppBarTitle(stringResource(titleRes)) },
                    navigateUp = navigateUp,
                    searchEnabled = searchEnabled,
                    searchQuery = if (searchEnabled) searchQuery else null,
                    onChangeSearchQuery = onChangeSearchQuery,
                    actions = {
                        AppBarActions(tab.actions)
                        ActiveProfileButton()
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { contentPadding ->
        Column(
            modifier = Modifier.padding(
                top = if (chromeVisible) contentPadding.calculateTopPadding() else 0.dp,
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            ),
        ) {
            if (chromeVisible) {
                TabRow(modifier = Modifier.zIndex(1f)) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = state.currentPage == index
                        val onClick: () -> Unit = { scope.launch { state.animateScrollToPage(index) } }
                        val label = tab.tabLabel
                        if (label != null) {
                            Tab(selected = selected, onClick = onClick, label = label)
                        } else {
                            Tab(
                                title = stringResource(tab.titleRes),
                                selected = selected,
                                onClick = onClick,
                                count = tab.badgeNumber,
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = state,
                userScrollEnabled = chromeVisible,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    snackbarHostState,
                )
            }
        }
    }
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val tabLabel: (@Composable () -> Unit)? = null,
    val chromeVisible: @Composable () -> Boolean = { true },
    val searchEnabled: Boolean = false,
    val actions: List<AppBar.AppBarAction> = listOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)

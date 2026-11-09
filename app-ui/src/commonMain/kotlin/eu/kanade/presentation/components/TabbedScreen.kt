package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.layout.FramedContent
import app.katari.presentation.kit.layout.ScreenFrame
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarTitle
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.navigation.SearchToolbar
import app.katari.presentation.kit.navigation.Tab
import app.katari.presentation.kit.navigation.TabRow
import app.katari.presentation.kit.pager.HorizontalPager
import app.katari.presentation.kit.shortcut.KeyboardShortcutScope
import app.katari.profiles.ui.switcher.ActiveProfileButton
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.launch
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
    // One frame for every tab, so the title and tabs keep their place when the tab changes
    frame: ScreenFrame = ScreenFrame.Rows,
    /** Actions of the whole screen, before the open tab's own. */
    screenActions: List<AppBar.AppBarAction> = emptyList(),
) {
    val scope = rememberCoroutineScope()
    val tab = tabs[state.currentPage]
    val chromeVisible = tab.chromeVisible()

    Scaffold(
        // A tab that hides the screen's bars, such as the immersive feed, lays out the whole window itself
        frame = if (chromeVisible) frame else ScreenFrame.Window,
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
                        AppBarActions(screenActions + tab.actions)
                        ActiveProfileButton()
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { contentPadding ->
        FramedContent(
            modifier = Modifier.padding(top = if (chromeVisible) contentPadding.calculateTopPadding() else 0.dp),
        ) {
            Column {
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
                    // The pager composes neighbouring pages ahead of showing them
                    KeyboardShortcutScope(active = page == state.currentPage) {
                        tabs[page].content(
                            PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                            snackbarHostState,
                        )
                    }
                }
            }
        }
    }
}

data class TabContent(
    val titleRes: StringResource,
    val badgeNumber: Int? = null,
    val tabLabel: (@Composable () -> Unit)? = null,
    /** Whether the screen's app bar and tabs show; while they don't, the tab has the whole window. */
    val chromeVisible: @Composable () -> Boolean = { true },
    val searchEnabled: Boolean = false,
    val actions: List<AppBar.AppBarAction> = listOf(),
    val content: @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)

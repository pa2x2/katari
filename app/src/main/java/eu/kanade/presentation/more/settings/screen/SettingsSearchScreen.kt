package eu.kanade.presentation.more.settings.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.collectLatest
import mihon.app.di.appGraph
import mihon.entry.interactions.media.EntryViewerSettingsFeature
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.SearchToolbar
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

class SettingsSearchScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val softKeyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val listState = rememberLazyListState()

        // Hide keyboard on change screen
        DisposableEffect(Unit) {
            onDispose {
                softKeyboardController?.hide()
            }
        }

        // Hide keyboard on outside text field is touched
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                focusManager.clearFocus()
            }
        }

        var query by rememberSaveable { mutableStateOf("") }
        Scaffold(
            topBar = {
                val canPop = remember { navigator.canPop }
                SearchToolbar(
                    searchQuery = query,
                    onChangeSearchQuery = { query = it.orEmpty() },
                    placeholderText = stringResource(MR.strings.action_search_settings),
                    onClickCloseSearch = if (canPop) {
                        { navigator.pop() }
                    } else {
                        null
                    },
                )
            },
        ) { contentPadding ->
            SearchResult(
                searchKey = { query },
                listState = listState,
                contentPadding = contentPadding,
            ) { result ->
                SearchableSettings.highlightKey = result.highlightKey
                navigator.replace(result.route)
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchKey: () -> String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    onItemClick: (SearchResultItem) -> Unit,
) {
    val context = LocalContext.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val viewerSettingsFeature = remember { context.appGraph.entryViewerSettingsFeature }
    val index = getIndex(viewerSettingsFeature)
    // The query is read here rather than in composition, so typing doesn't rebuild the index on every key
    val result by produceState<List<SearchResultItem>?>(initialValue = null, index) {
        snapshotFlow(searchKey).collectLatest { searchKey ->
            value = if (searchKey.isEmpty()) null else search(index, searchKey, isLtr)
        }
    }

    Crossfade(
        targetState = result,
        label = "results",
    ) {
        when {
            it == null -> {}
            it.isEmpty() -> NoResults(modifier = Modifier.padding(contentPadding))
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    rowGroupItems(
                        items = it,
                        key = { i -> i.hashCode() },
                    ) { item ->
                        SettingsRow(
                            title = item.title,
                            subtitle = item.breadcrumbs,
                            subtitleMaxLines = 1,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}

private fun search(index: List<SettingsData>, searchKey: String, isLtr: Boolean): List<SearchResultItem> {
    return index.asSequence()
        .flatMap { settingsData ->
            fun result(title: String, categoryTitle: String?) = SearchResultItem(
                route = settingsData.route,
                title = title,
                breadcrumbs = getLocalizedBreadcrumb(
                    path = settingsData.title,
                    node = categoryTitle,
                    isLtr = isLtr,
                ),
                highlightKey = title,
            )

            fun Preference.PreferenceItem<*, *>.matches(): Boolean {
                // Don't show info preference
                if (this is Preference.PreferenceItem.InfoPreference) return false
                val inTitle = title.contains(searchKey, true)
                val inSummary = subtitle?.contains(searchKey, true) ?: false
                return inTitle || inSummary
            }

            settingsData.contents.asSequence()
                // Only search from visible prefs and one with valid title
                .filter { it.visible && it.title.isNotBlank() }
                .flatMap { p ->
                    when (p) {
                        is Preference.PreferenceGroup -> {
                            // A matching heading leads to the group itself, not to one of its items
                            val heading = if (p.title.contains(searchKey, true)) {
                                sequenceOf(result(p.title, null))
                            } else {
                                emptySequence()
                            }
                            val items = p.preferenceItems.asSequence()
                                .filter { it.visible && it.title.isNotBlank() && it.matches() }
                                .map { result(it.title, p.title) }
                            heading + items
                        }
                        is Preference.PreferenceItem<*, *> -> {
                            if (p.matches()) sequenceOf(result(p.title, null)) else emptySequence()
                        }
                    }
                }
        }
        .take(10) // Just take top 10 result for quicker result
        .toList()
}

@Composable
@NonRestartableComposable
private fun getIndex(viewerSettingsFeature: EntryViewerSettingsFeature) = settingScreens
    .plus(
        viewerProviderSettingsScreens(viewerSettingsFeature),
    )
    .distinct()
    .filter { isSettingsScreenVisible(it) }
    .map { screen ->
        SettingsData(
            title = stringResource(screen.getTitleRes()),
            route = screen,
            contents = screen.getPreferences(),
        )
    }

private fun getLocalizedBreadcrumb(path: String, node: String?, isLtr: Boolean): String {
    return if (node == null) {
        path
    } else {
        if (isLtr) {
            // This locale reads left to right.
            "$path > $node"
        } else {
            // This locale reads right to left.
            "$node < $path"
        }
    }
}

private val settingScreens = listOf(
    SettingsAppearanceScreen,
    SettingsLibraryScreen,
    SettingsLibraryUpdatesScreen,
    SettingsReaderScreen,
    SettingsPlayerScreen,
    SettingsDownloadScreen,
    SettingsTrackingScreen,
    SettingsTranslationScreen,
    SettingsTtsScreen,
    SettingsTextRecognitionScreen,
    SettingsBrowseScreen,
    SettingsDataScreen,
    SettingsSecurityScreen,
    SettingsProfilesScreen,
    SettingsAdvancedScreen,
)

private data class SettingsData(
    val title: String,
    val route: VoyagerScreen,
    val contents: List<Preference>,
)

private data class SearchResultItem(
    val route: VoyagerScreen,
    val title: String,
    val breadcrumbs: String,
    val highlightKey: String,
)

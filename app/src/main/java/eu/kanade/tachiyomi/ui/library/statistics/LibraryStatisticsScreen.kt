package eu.kanade.tachiyomi.ui.library.statistics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.statistics.LibraryStatisticsScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data class LibraryStatisticsScreen(
    private val filter: LibraryStatisticsFilter,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { LibraryStatisticsScreenModel(filter) }
        val state by screenModel.state.collectAsState()
        val title = when (filter.kind) {
            LibraryStatisticsFilterKind.LIBRARY -> MR.strings.statistics_library
            LibraryStatisticsFilterKind.OFFLINE -> MR.strings.statistics_available_offline
            LibraryStatisticsFilterKind.TRACKED -> MR.strings.pref_category_tracking
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            LibraryStatisticsScreenContent(
                state = state,
                paddingValues = paddingValues,
                showType = filter.type == null,
                onEntryClick = { navigator.push(EntryScreen(it)) },
            )
        }
    }
}

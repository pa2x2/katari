package eu.kanade.tachiyomi.ui.stats.top

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.more.stats.top.StatisticsTopTitlesScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The full ranking behind the dashboard's Top titles card. [totalDurationMillis] is the window's total, so
 * shares match the card even though the list loads in pages.
 */
data class StatisticsTopTitlesScreen(
    private val typeName: String?,
    private val startLocalDate: String?,
    private val endLocalDate: String,
    private val period: String,
    private val totalDurationMillis: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<StatisticsTopTitlesViewModel, StatisticsTopTitlesViewModel.Factory> {
            create(typeName, startLocalDate, endLocalDate)
        }
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_top_titles),
                    subtitle = period,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            StatisticsTopTitlesScreenContent(
                state = state,
                selectedType = viewModel.type,
                types = viewModel.types,
                totalDurationMillis = totalDurationMillis,
                paddingValues = paddingValues,
                onLoadMore = viewModel::loadMore,
                onEntryClick = { navigator.push(EntryScreen(it)) },
            )
        }
    }
}

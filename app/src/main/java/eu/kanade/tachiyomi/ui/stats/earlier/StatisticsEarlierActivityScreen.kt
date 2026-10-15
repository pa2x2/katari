package eu.kanade.tachiyomi.ui.stats.earlier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.stats.earlier.StatisticsEarlierActivityScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class StatisticsEarlierActivityScreen(
    private val typeName: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { StatisticsEarlierActivityViewModel(typeName) }
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_earlier_from_history),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            StatisticsEarlierActivityScreenContent(
                state = state,
                selectedType = viewModel.type,
                types = viewModel.types,
                paddingValues = paddingValues,
                onEntryClick = { navigator.push(EntryScreen(it)) },
            )
        }
    }
}

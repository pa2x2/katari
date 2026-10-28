package eu.kanade.tachiyomi.ui.stats.recap.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.stats.recap.list.StatisticsRecapsScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class StatisticsRecapsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<StatisticsRecapsViewModel>()
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_recaps),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            val loaded = state
            if (loaded == null) {
                LoadingScreen()
                return@Scaffold
            }
            StatisticsRecapsScreenContent(
                state = loaded,
                paddingValues = paddingValues,
                onOpen = { navigator.push(StatisticsRecapScreen(it)) },
            )
        }
    }
}

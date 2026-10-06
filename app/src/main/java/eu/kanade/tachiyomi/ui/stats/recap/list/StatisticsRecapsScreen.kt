package eu.kanade.tachiyomi.ui.stats.recap.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.stats.recap.list.StatisticsRecapsScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class StatisticsRecapsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { StatisticsRecapsScreenModel() }
        val state by screenModel.state.collectAsState()

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

package eu.kanade.tachiyomi.ui.stats.earlier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.stats.earlier.StatisticsEarlierActivityScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data class StatisticsEarlierActivityScreen(
    private val typeName: String?,
    private val trackingStartedAtEpochMillis: Long?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { StatisticsEarlierActivityScreenModel(typeName) }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_earlier_activity),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            StatisticsEarlierActivityScreenContent(
                state = state,
                selectedType = screenModel.type,
                types = screenModel.types,
                trackingStartedAtEpochMillis = trackingStartedAtEpochMillis,
                paddingValues = paddingValues,
                onEntryClick = { navigator.push(EntryScreen(it)) },
            )
        }
    }
}

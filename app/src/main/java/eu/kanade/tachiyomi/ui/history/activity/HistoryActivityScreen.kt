package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.history.activity.HistoryActivityScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

data class HistoryActivityScreen(
    private val startLocalDate: String,
    private val endLocalDate: String,
    private val typeName: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            HistoryActivityScreenModel(startLocalDate, endLocalDate, typeName)
        }
        val state by screenModel.state.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.statistics_activity),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            HistoryActivityScreenContent(
                state = state,
                startLocalDate = startLocalDate,
                endLocalDate = endLocalDate,
                type = screenModel.type,
                paddingValues = paddingValues,
                onEntryClick = { navigator.push(EntryScreen(it)) },
                onRetry = screenModel::retry,
                onLoadMore = screenModel::loadMore,
            )
        }
    }
}

package eu.kanade.tachiyomi.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.stats.StatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreen
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { StatsScreenModel() }
        val state by screenModel.state.collectAsState()
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner, screenModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) screenModel.refreshToday()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_stats),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            if (state is StatsScreenState.Loading) {
                LoadingScreen()
                return@Scaffold
            }

            StatsScreenContent(
                state = state as StatsScreenState.Success,
                paddingValues = paddingValues,
                onRangeSelected = screenModel::setRange,
                onTypeSelected = screenModel::setType,
                onNavigateActivity = screenModel::navigateActivityByBuckets,
                onShowToday = screenModel::showToday,
                onRetryActivity = screenModel::retryActivity,
                onOpenActivity = { type, point ->
                    navigator.push(
                        HistoryActivityScreen(
                            startLocalDate = point.startDate.toString(),
                            endLocalDate = point.endDate.toString(),
                            typeName = type?.name,
                        ),
                    )
                },
                onOpenEntry = { navigator.push(EntryScreen(it)) },
                onOpenEarlierActivity = { type, trackingStartedAtEpochMillis ->
                    navigator.push(
                        StatisticsEarlierActivityScreen(
                            typeName = type?.name,
                            trackingStartedAtEpochMillis = trackingStartedAtEpochMillis,
                        ),
                    )
                },
            )
        }
    }
}

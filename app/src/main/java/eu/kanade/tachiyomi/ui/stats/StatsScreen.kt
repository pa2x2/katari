package eu.kanade.tachiyomi.ui.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.stats.StatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.components.formatStatisticsWindow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreen
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityScreen
import eu.kanade.tachiyomi.ui.stats.top.StatisticsTopTitlesScreen
import tachiyomi.i18n.*
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

        var customizing by rememberSaveable { mutableStateOf(false) }
        val allActivity = stringResource(MR.strings.statistics_all_activity)

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_stats),
                    navigateUp = navigator::pop,
                    actions = {
                        if (state is StatsScreenState.Success) {
                            AppBarActions(
                                listOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.statistics_customize),
                                        icon = Icons.Outlined.Tune,
                                        onClick = { customizing = true },
                                    ),
                                ),
                            )
                        }
                    },
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
                customizing = customizing,
                onCustomizingChange = { customizing = it },
                onRangeSelected = screenModel::setRange,
                onSaveLayout = screenModel::setCardLayout,
                onSaveDailyGoal = screenModel::setDailyGoal,
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
                onOpenTopTitles = { type, activity ->
                    navigator.push(
                        StatisticsTopTitlesScreen(
                            typeName = type?.name,
                            startLocalDate = activity.window.startDate?.toString(),
                            endLocalDate = activity.window.endDate.toString(),
                            period = if (activity.window.startDate ==
                                null
                            ) {
                                allActivity
                            } else {
                                formatStatisticsWindow(activity.window)
                            },
                            totalDurationMillis = activity.totalDurationMillis,
                        ),
                    )
                },
                onOpenEarlierActivity = { type ->
                    navigator.push(
                        StatisticsEarlierActivityScreen(
                            typeName = type?.name,
                        ),
                    )
                },
            )
        }
    }
}

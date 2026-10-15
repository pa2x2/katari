package eu.kanade.tachiyomi.ui.stats

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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.StatsScreenContent
import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.presentation.more.stats.components.formatStatisticsWindow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreen
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityScreen
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapScreen
import eu.kanade.tachiyomi.ui.stats.recap.list.StatisticsRecapsScreen
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.recapPeriodTitle
import eu.kanade.tachiyomi.ui.stats.top.StatisticsTopTitlesScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class StatsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<StatsViewModel>()
        val state by viewModel.state.collectAsState()
        val newYearRecap by viewModel.newYearRecap.collectAsState()
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner, viewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshToday()
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
                        val success = state as? StatsScreenState.Success
                        if (success != null) {
                            val shownWindow = (success.activity as? ActivityState.Available)?.data?.window
                            AppBarActions(
                                listOfNotNull(
                                    shownWindow?.let { window ->
                                        AppBar.Action(
                                            title = stringResource(MR.strings.statistics_share_recap),
                                            icon = MaterialSymbols.Rounded.Share,
                                            onClick = {
                                                navigator.push(
                                                    StatisticsRecapScreen(
                                                        StatisticsRecapPeriod.Window(
                                                            startDate = window.startDate,
                                                            end = window.endDate,
                                                            type = success.selectedType,
                                                            label = if (window.startDate == null) {
                                                                allActivity
                                                            } else {
                                                                formatStatisticsWindow(window)
                                                            },
                                                        ),
                                                    ),
                                                )
                                            },
                                        )
                                    },
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.statistics_customize),
                                        onClick = { customizing = true },
                                    ),
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.statistics_recaps),
                                        onClick = { navigator.push(StatisticsRecapsScreen()) },
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
                onRangeSelected = viewModel::setRange,
                onSaveLayout = viewModel::setCardLayout,
                onSaveDailyGoal = viewModel::setDailyGoal,
                onSaveRecapNotifications = viewModel::setRecapNotifications,
                newYearRecapTitle = newYearRecap?.let { recapPeriodTitle(it) },
                onOpenNewYearRecap = { newYearRecap?.let { navigator.push(StatisticsRecapScreen(it)) } },
                onTypeSelected = viewModel::setType,
                onNavigateActivity = viewModel::navigateActivityByBuckets,
                onPageActivity = viewModel::navigateActivityByWindows,
                onShowToday = viewModel::showToday,
                onRetryActivity = viewModel::retryActivity,
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

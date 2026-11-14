package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.history.activity.HistoryActivityScreenContent
import eu.kanade.presentation.more.stats.components.formatStatisticsDay
import eu.kanade.presentation.more.stats.components.formatStatisticsDays
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate

/**
 * Activity within a date range, day by day, for every title, one type, or one title ([entryIds] with its
 * [entryTitle]).
 */
data class HistoryActivityScreen(
    private val startLocalDate: String,
    private val endLocalDate: String,
    private val typeName: String? = null,
    private val entryIds: List<Long>? = null,
    private val entryTitle: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<HistoryActivityViewModel, HistoryActivityViewModel.Factory> {
            create(startLocalDate, endLocalDate, typeName, entryIds)
        }
        val state by viewModel.state.collectAsState()
        val summary by viewModel.summary.collectAsState()
        val context = currentPlatformContext()
        val locale = LocalLocale.current.platformLocale
        val period = remember(startLocalDate, endLocalDate, context, locale) {
            val start = LocalDate.parse(startLocalDate)
            val end = LocalDate.parse(endLocalDate)
            val withYear = start.year != LocalDate.now().year || end.year != LocalDate.now().year
            if (start == end) {
                context.formatStatisticsDay(start, withYear)
            } else {
                context.formatStatisticsDays(start, end, withYear)
            }
        }
        val scopeName = viewModel.type?.let { stringResource(it.entryTypePresentation().displayNameLabel) }
            ?: stringResource(MR.strings.statistics_all_activity)

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = entryTitle ?: period,
                    subtitle = if (entryTitle != null) period else scopeName,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            HistoryActivityScreenContent(
                state = state,
                summary = summary,
                types = viewModel.types,
                oneTitle = viewModel.isEntryScope,
                paddingValues = paddingValues,
                onEntryClick = { navigator.push(EntryScreen(it)) },
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
            )
        }
    }
}

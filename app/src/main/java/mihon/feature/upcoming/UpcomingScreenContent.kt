package mihon.feature.upcoming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.relativeDateText
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import mihon.feature.upcoming.components.UpcomingItem
import mihon.feature.upcoming.components.calendar.Calendar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.layout.TwoPanes
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarScrollBehavior
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.core.common.Constants
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun UpcomingScreenContent(
    state: UpcomingViewModel.State,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickUpcoming: (entry: Entry) -> Unit,
    onClickFilter: () -> Unit,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val onClickDay: (LocalDate, Int) -> Unit = { date, offset ->
        state.headerIndexes[date]?.let {
            scope.launch {
                listState.animateScrollToItem(it + offset)
            }
        }
    }
    Scaffold(
        topBar = { scrollBehavior ->
            UpcomingToolbar(
                hasFilters = hasActiveFilters,
                onClickFilter = onClickFilter,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (currentWindowLayout().usesTwoPanes) {
            UpcomingScreenLargeImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 0) },
                onClickUpcoming = onClickUpcoming,
            )
        } else {
            UpcomingScreenSmallImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 1) },
                onClickUpcoming = onClickUpcoming,
            )
        }
    }
}

@Composable
private fun UpcomingToolbar(
    hasFilters: Boolean,
    onClickFilter: () -> Unit,
    scrollBehavior: AppBarScrollBehavior,
) {
    val navigator = LocalNavigator.currentOrThrow
    val uriHandler = LocalUriHandler.current

    AppBar(
        title = stringResource(MR.strings.label_upcoming),
        navigateUp = navigator::pop,
        scrollBehavior = scrollBehavior,
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = MaterialSymbols.Rounded.FilterList,
                        tone = if (hasFilters) ContentTone.Active else ContentTone.Inherit,
                        onClick = onClickFilter,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.upcoming_guide),
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        onClick = { uriHandler.openUri(Constants.URL_HELP_UPCOMING) },
                    ),
                ),
            )
        },
    )
}

@Composable
private fun DateHeading(
    date: LocalDate,
    entryCount: Int,
) {
    RowGroupHeader(
        text = relativeDateText(date),
        trailing = { Badge(text = entryCount.toString(), tone = BadgeTone.Primary) },
    )
}

@Composable
private fun UpcomingScreenSmallImpl(
    listState: LazyListState,
    items: List<UpcomingUIModel>,
    events: Map<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (entry: Entry) -> Unit,
) {
    val positions = remember(items) { upcomingDayPositions(items) }
    RowList(
        fastScroll = true,
        contentPadding = paddingValues,
        state = listState,
    ) {
        item(key = "upcoming-calendar") {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        }
        upcomingDays(items = items, positions = positions, onClickUpcoming = onClickUpcoming)
    }
}

@Composable
private fun UpcomingScreenLargeImpl(
    listState: LazyListState,
    items: List<UpcomingUIModel>,
    events: Map<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (entry: Entry) -> Unit,
) {
    val positions = remember(items) { upcomingDayPositions(items) }
    TwoPanes(
        modifier = Modifier.padding(paddingValues),
        start = {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        },
        end = {
            RowList(fastScroll = true, state = listState) {
                upcomingDays(items = items, positions = positions, onClickUpcoming = onClickUpcoming)
            }
        },
    )
}

/** One card per release day under its date; the calendar scrolls to a day by the index of its header. */
private fun LazyListScope.upcomingDays(
    items: List<UpcomingUIModel>,
    positions: List<RowGroupPosition?>,
    onClickUpcoming: (entry: Entry) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = { _, item -> "upcoming-${item.hashCode()}" },
        contentType = { _, item ->
            when (item) {
                is UpcomingUIModel.Header -> "header"
                is UpcomingUIModel.Item -> "item"
            }
        },
    ) { index, item ->
        when (item) {
            is UpcomingUIModel.Item -> {
                RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone) {
                    UpcomingItem(
                        upcoming = item.entry,
                        notCheckedReason = item.notCheckedReason,
                        onClick = { onClickUpcoming(item.entry) },
                    )
                }
            }

            is UpcomingUIModel.Header -> {
                DateHeading(
                    date = item.date,
                    entryCount = item.entryCount,
                )
            }
        }
    }
}

private fun upcomingDayPositions(items: List<UpcomingUIModel>) =
    RowGroupPosition.ofRuns(items) { it is UpcomingUIModel.Item }

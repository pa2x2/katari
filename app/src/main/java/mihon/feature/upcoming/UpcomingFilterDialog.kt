package mihon.feature.upcoming

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.kanade.presentation.category.visualName
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.InfoRow
import mihon.presentation.kit.list.TriStateRow
import mihon.presentation.kit.sheet.TabbedSheet
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

@Composable
private fun CategoryFilterSheet(
    viewModel: UpcomingViewModel,
) {
    InfoRow(text = stringResource(MR.strings.pref_filter_upcoming_categories_details), showIcon = false)
    HorizontalDivider()

    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val profileId = screenState.profileId
    if (profileId == null) {
        LoadingScreen(modifier = Modifier.padding(KatariTheme.spacing.l))
        return
    }
    val allCategories by viewModel.getCategories
        .subscribeForProfile(profileId)
        .collectFlowAsState(initial = emptyList())

    if (allCategories.isEmpty()) {
        LoadingScreen(modifier = Modifier.padding(KatariTheme.spacing.l))
        return
    }

    val excluded by viewModel.upcomingPreferences.filterExcludedCategories.collectAsState()
    val included by viewModel.upcomingPreferences.filterIncludedCategories.collectAsState()
    val selected = remember(allCategories, included, excluded) {
        allCategories.map { category ->
            when (category.id) {
                in excluded -> TriState.ENABLED_NOT
                in included -> TriState.ENABLED_IS
                else -> TriState.DISABLED
            }
        }.toMutableStateList()
    }

    allCategories.fastForEachIndexed { index, category ->
        val state = selected[index]
        TriStateRow(
            title = category.visualName,
            state = state,
            onStateChange = {
                selected[index] = state.next()
                viewModel.cycleCategory(category)
            },
        )
    }
}

@Composable
fun UpcomingFilterDialog(
    viewModel: UpcomingViewModel,
) {
    TabbedSheet(
        onDismissRequest = viewModel::resetDialog,
        tabTitles = listOf(stringResource(MR.strings.categories)),
    ) {
        CategoryFilterSheet(viewModel = viewModel)
    }
}

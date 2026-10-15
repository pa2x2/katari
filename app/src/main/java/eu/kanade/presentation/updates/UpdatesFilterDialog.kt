package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsViewModel
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.InfoRow
import mihon.presentation.kit.list.TriStateRow
import mihon.presentation.kit.sheet.TabbedSheet
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.model.UpdatesFeedFilter
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * The Updates feed's filters. Types and sources get their own tab only when the feed has more than one to choose
 * between, or one is already filtered by.
 *
 * @param entryTypes the types the feed's updates have, plus any already filtered by.
 * @param sources names of the sources the feed's updates come from, plus any already filtered by, by id.
 */
@Composable
fun UpdatesFilterDialog(
    onDismissRequest: () -> Unit,
    viewModel: UpdatesSettingsViewModel,
    filter: UpdatesFeedFilter,
    entryTypes: List<EntryType>,
    sources: Map<Long, String>,
    options: List<UpdatesFilterOption> = unifiedUpdatesFilterOptions(),
) {
    val tabs = buildList {
        add(UpdatesFilterTab.Filter)
        add(UpdatesFilterTab.Categories)
        if (entryTypes.size > 1 || filter.types.isActive) add(UpdatesFilterTab.Types)
        if (sources.size > 1 || filter.sources.isActive) add(UpdatesFilterTab.Sources)
    }
    TabbedSheet(
        onDismissRequest = onDismissRequest,
        tabTitles = tabs.map { stringResource(it.title) },
    ) { page ->
        when (tabs[page]) {
            UpdatesFilterTab.Filter -> FilterSheet(
                viewModel = viewModel,
                options = options,
            )
            UpdatesFilterTab.Categories -> CategoryFilterSheet(viewModel = viewModel)
            UpdatesFilterTab.Types -> entryTypes.forEach { type ->
                TriStateRow(
                    title = stringResource(type.entryTypePresentation().displayNameLabel),
                    state = filter.types.stateOf(type),
                    onStateChange = { viewModel.cycleEntryType(type) },
                )
            }
            UpdatesFilterTab.Sources ->
                sources.entries
                    .sortedBy { it.value.lowercase() }
                    .forEach { (sourceId, name) ->
                        TriStateRow(
                            title = name,
                            state = filter.sources.stateOf(sourceId),
                            onStateChange = { viewModel.cycleSource(sourceId) },
                        )
                    }
        }
    }
}

private enum class UpdatesFilterTab(val title: StringResource) {
    Filter(MR.strings.action_filter),
    Categories(MR.strings.categories),
    Types(MR.strings.library_updates_types),
    Sources(MR.strings.label_sources),
}

fun unifiedUpdatesFilterOptions(): List<UpdatesFilterOption> {
    return listOf(
        UpdatesFilterOption(
            label = MR.strings.label_downloaded,
            preference = UpdatesPreferences::filterDownloaded,
        ),
        UpdatesFilterOption(
            label = MR.strings.action_filter_unseen,
            preference = UpdatesPreferences::filterUnread,
        ),
        UpdatesFilterOption(
            label = MR.strings.label_started,
            preference = UpdatesPreferences::filterStarted,
        ),
        UpdatesFilterOption(
            label = MR.strings.action_filter_bookmarked,
            preference = UpdatesPreferences::filterBookmarked,
        ),
    )
}

@Composable
private fun FilterSheet(
    viewModel: UpdatesSettingsViewModel,
    options: List<UpdatesFilterOption>,
) {
    options.forEach { option ->
        val state by option.preference(viewModel.updatesPreferences).collectAsState()
        TriStateRow(
            title = stringResource(option.label),
            state = state,
            onStateChange = { viewModel.toggleFilter(option.preference) },
        )
    }
}

data class UpdatesFilterOption(
    val label: StringResource,
    val preference: (UpdatesPreferences) -> Preference<TriState>,
)

@Composable
private fun CategoryFilterSheet(
    viewModel: UpdatesSettingsViewModel,
) {
    InfoRow(text = stringResource(MR.strings.pref_filter_update_categories_details), showIcon = false)
    HorizontalDivider()

    val allCategories by viewModel.getCategories.subscribe().collectAsState(initial = emptyList())

    if (allCategories.isEmpty()) {
        // since it includes the system category, this should only happen when loading is required
        LoadingScreen(modifier = Modifier.padding(KatariTheme.spacing.l))
        return
    }

    val excluded by viewModel.updatesPreferences.filterExcludedCategories.collectAsState()
    val included by viewModel.updatesPreferences.filterIncludedCategories.collectAsState()

    allCategories.fastForEach { category ->
        val state = when (category.id) {
            in excluded -> TriState.ENABLED_NOT
            in included -> TriState.ENABLED_IS
            else -> TriState.DISABLED
        }
        TriStateRow(
            title = category.visualName,
            state = state,
            onStateChange = { viewModel.cycleCategory(category) },
        )
    }
}

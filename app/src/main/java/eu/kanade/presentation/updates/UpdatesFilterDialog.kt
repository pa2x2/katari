package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsScreenModel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.model.UpdatesFeedFilter
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
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
    screenModel: UpdatesSettingsScreenModel,
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
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = tabs.map { stringResource(it.title) },
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (tabs[page]) {
                UpdatesFilterTab.Filter -> FilterSheet(
                    screenModel = screenModel,
                    options = options,
                )
                UpdatesFilterTab.Categories -> CategoryFilterSheet(screenModel = screenModel)
                UpdatesFilterTab.Types -> entryTypes.forEach { type ->
                    TriStateItem(
                        label = stringResource(type.entryTypePresentation().displayNameLabel),
                        state = filter.types.stateOf(type),
                        onClick = { screenModel.cycleEntryType(type) },
                    )
                }
                UpdatesFilterTab.Sources ->
                    sources.entries
                        .sortedBy { it.value.lowercase() }
                        .forEach { (sourceId, name) ->
                            TriStateItem(
                                label = name,
                                state = filter.sources.stateOf(sourceId),
                                onClick = { screenModel.cycleSource(sourceId) },
                            )
                        }
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
    screenModel: UpdatesSettingsScreenModel,
    options: List<UpdatesFilterOption>,
) {
    options.forEach { option ->
        val state by option.preference(screenModel.updatesPreferences).collectAsState()
        TriStateItem(
            label = stringResource(option.label),
            state = state,
            onClick = { screenModel.toggleFilter(option.preference) },
        )
    }
}

data class UpdatesFilterOption(
    val label: StringResource,
    val preference: (UpdatesPreferences) -> Preference<TriState>,
)

@Composable
private fun ColumnScope.CategoryFilterSheet(
    screenModel: UpdatesSettingsScreenModel,
) {
    Text(
        stringResource(MR.strings.pref_filter_update_categories_details),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsItemsPaddings.Horizontal,
                vertical = SettingsItemsPaddings.Vertical,
            ),
    )

    HorizontalDivider(modifier = Modifier.padding(MaterialTheme.padding.extraSmall))

    val allCategories by screenModel.getCategories.subscribe().collectAsState(initial = emptyList())

    if (allCategories.isEmpty()) {
        // since it includes the system category, this should only happen when loading is required
        LoadingScreen(modifier = Modifier.padding(16.dp))
        return
    }

    val excluded by screenModel.updatesPreferences.filterExcludedCategories.collectAsState()
    val included by screenModel.updatesPreferences.filterIncludedCategories.collectAsState()

    Column {
        allCategories.fastForEach { category ->
            val state = when (category.id) {
                in excluded -> TriState.ENABLED_NOT
                in included -> TriState.ENABLED_IS
                else -> TriState.DISABLED
            }
            TriStateItem(
                label = category.visualName,
                state = state,
                onClick = {
                    screenModel.cycleCategory(category)
                },
            )
        }
    }
}

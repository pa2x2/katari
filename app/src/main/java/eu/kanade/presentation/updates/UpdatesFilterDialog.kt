package eu.kanade.presentation.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsScreenModel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.updates.service.UpdatesPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun UpdatesFilterDialog(
    onDismissRequest: () -> Unit,
    screenModel: UpdatesSettingsScreenModel,
    options: List<UpdatesFilterOption> = unifiedUpdatesFilterOptions(),
) {
    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = listOf(
            stringResource(MR.strings.action_filter),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            FilterSheet(
                screenModel = screenModel,
                options = options,
            )
        }
    }
}

fun unifiedUpdatesFilterOptions(): List<UpdatesFilterOption> {
    return listOf(
        UpdatesFilterOption.TriStateOption(
            label = MR.strings.label_downloaded,
            preference = UpdatesPreferences::filterDownloaded,
        ),
        UpdatesFilterOption.TriStateOption(
            label = MR.strings.action_filter_unconsumed,
            preference = UpdatesPreferences::filterUnread,
        ),
        UpdatesFilterOption.TriStateOption(
            label = MR.strings.label_started,
            preference = UpdatesPreferences::filterStarted,
        ),
        UpdatesFilterOption.TriStateOption(
            label = MR.strings.action_filter_bookmarked,
            preference = UpdatesPreferences::filterBookmarked,
        ),
        UpdatesFilterOption.SwitchOption(
            label = MR.strings.action_filter_excluded_scanlators,
            preference = UpdatesPreferences::filterExcludedScanlators,
            showDividerBefore = true,
        ),
    )
}

@Composable
private fun ColumnScope.FilterSheet(
    screenModel: UpdatesSettingsScreenModel,
    options: List<UpdatesFilterOption>,
) {
    options.forEach { option ->
        when (option) {
            is UpdatesFilterOption.TriStateOption -> {
                val state by option.preference(screenModel.updatesPreferences).collectAsState()
                TriStateItem(
                    label = stringResource(option.label),
                    state = state,
                    onClick = { screenModel.toggleFilter(option.preference) },
                )
            }

            is UpdatesFilterOption.SwitchOption -> {
                if (option.showDividerBefore) {
                    HorizontalDivider(modifier = Modifier.padding(MaterialTheme.padding.small))
                }

                val checked by option.preference(screenModel.updatesPreferences).collectAsState()

                fun toggleSwitch() {
                    option.preference(screenModel.updatesPreferences).getAndSet { !it }
                }

                Row(
                    modifier = Modifier
                        .clickable { toggleSwitch() }
                        .fillMaxWidth()
                        .padding(horizontal = SettingsItemsPaddings.Horizontal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(option.label),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Switch(
                        checked = checked,
                        onCheckedChange = { toggleSwitch() },
                    )
                }
            }
        }
    }
}

sealed interface UpdatesFilterOption {
    data class TriStateOption(
        val label: StringResource,
        val preference: (UpdatesPreferences) -> Preference<TriState>,
    ) : UpdatesFilterOption

    data class SwitchOption(
        val label: StringResource,
        val preference: (UpdatesPreferences) -> Preference<Boolean>,
        val showDividerBefore: Boolean = false,
    ) : UpdatesFilterOption
}

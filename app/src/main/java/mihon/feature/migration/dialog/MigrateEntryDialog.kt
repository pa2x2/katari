package mihon.feature.migration.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.entry.entryTypePresentation
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.EntryMigrationExecuteIntent
import mihon.entry.interactions.EntryMigrationExecutionResult
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationMode
import mihon.entry.interactions.EntryMigrationOption
import mihon.entry.interactions.EntryMigrationPreparationResult
import mihon.entry.interactions.EntryMigrationPrepareIntent
import mihon.entry.interactions.EntryMigrationReference
import mihon.feature.migration.options.getLabel
import mihon.feature.migration.options.toEntryMigrationOptions
import mihon.feature.migration.options.toMigrationFlag
import mihon.feature.migration.options.toMigrationFlags
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun Screen.MigrateEntryDialog(
    current: Entry,
    target: Entry,
    onClickTitle: () -> Unit,
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit = onDismissRequest,
) {
    val scope = rememberCoroutineScope()

    val screenModel = rememberScreenModel { MigrateEntryDialogScreenModel() }
    LaunchedEffect(current, target) {
        screenModel.init(current, target)
    }
    val state by screenModel.state.collectAsState()

    if (state.isMigrated) return

    if (state.isLoading || state.isMigrating) {
        LoadingScreen(
            modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.migration_dialog_what_to_include))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                state.availableOptions.fastForEach { option ->
                    LabeledCheckbox(
                        label = stringResource(
                            when (option) {
                                EntryMigrationOption.CHILD_STATE ->
                                    current.type
                                        .entryTypePresentation()
                                        .childListTitle
                                else -> option.toMigrationFlag().getLabel()
                            },
                        ),
                        checked = option in state.selectedOptions,
                        onCheckedChange = { screenModel.toggleSelection(option) },
                    )
                }
                if (state.hasFailure) {
                    Text(
                        text = stringResource(MR.strings.internal_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onClickTitle()
                    },
                ) {
                    Text(text = stringResource(MR.strings.action_show_manga))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        scope.launchIO {
                            if (screenModel.migrateEntry(EntryMigrationMode.COPY)) {
                                withUIContext { onComplete() }
                            }
                        }
                    },
                    enabled = state.reference != null,
                ) {
                    Text(text = stringResource(MR.strings.copy))
                }
                TextButton(
                    onClick = {
                        scope.launchIO {
                            if (screenModel.migrateEntry(EntryMigrationMode.REPLACE)) {
                                withUIContext { onComplete() }
                            }
                        }
                    },
                    enabled = state.reference != null,
                ) {
                    Text(text = stringResource(MR.strings.migrate))
                }
            }
        },
    )
}

internal class MigrateEntryDialogScreenModel(
    private val sourcePreference: SourcePreferences = Injekt.get(),
    private val migration: EntryMigrationFeature = Injekt.get(),
) : StateScreenModel<MigrateEntryDialogScreenModel.State>(State()) {
    private var source: Entry? = null
    private var target: Entry? = null

    suspend fun init(current: Entry, target: Entry) {
        source = current
        this.target = target
        mutableState.update { State(isLoading = true) }
        when (val result = migration.prepare(EntryMigrationPrepareIntent(current, target))) {
            is EntryMigrationPreparationResult.Ready -> {
                val defaults = sourcePreference.migrationFlags.get().toEntryMigrationOptions()
                val selectedOptions = defaults.intersect(result.availableOptions)
                mutableState.update {
                    State(
                        reference = result.reference,
                        availableOptions = result.availableOptions.toList(),
                        selectedOptions = selectedOptions,
                        preservedDefaults = defaults - result.availableOptions,
                        isLoading = false,
                    )
                }
            }
            is EntryMigrationPreparationResult.OperationalFailure,
            is EntryMigrationPreparationResult.Rejected,
            -> mutableState.update { State(isLoading = false, hasFailure = true) }
        }
    }

    fun toggleSelection(option: EntryMigrationOption) {
        mutableState.update {
            val selectedOptions = it.selectedOptions.toMutableSet()
                .apply { if (contains(option)) remove(option) else add(option) }
                .toSet()
            it.copy(selectedOptions = selectedOptions)
        }
    }

    suspend fun migrateEntry(mode: EntryMigrationMode): Boolean {
        val state = state.value
        val reference = state.reference ?: return false
        sourcePreference.migrationFlags.set((state.selectedOptions + state.preservedDefaults).toMigrationFlags())
        mutableState.update { it.copy(isMigrating = true, hasFailure = false) }
        return when (
            migration.execute(
                EntryMigrationExecuteIntent(
                    reference = reference,
                    mode = mode,
                    selectedOptions = state.selectedOptions,
                ),
            )
        ) {
            is EntryMigrationExecutionResult.Applied -> {
                mutableState.update { it.copy(isMigrating = false, isMigrated = true) }
                true
            }
            EntryMigrationExecutionResult.Conflict -> {
                val source = source
                val target = target
                if (source != null && target != null) {
                    init(source, target)
                } else {
                    mutableState.update { it.copy(isMigrating = false, hasFailure = true) }
                }
                false
            }
            is EntryMigrationExecutionResult.OperationalFailure,
            is EntryMigrationExecutionResult.Rejected,
            -> {
                mutableState.update { it.copy(isMigrating = false, hasFailure = true) }
                false
            }
        }
    }

    data class State(
        val reference: EntryMigrationReference? = null,
        val availableOptions: List<EntryMigrationOption> = emptyList(),
        val selectedOptions: Set<EntryMigrationOption> = emptySet(),
        val preservedDefaults: Set<EntryMigrationOption> = emptySet(),
        val isLoading: Boolean = true,
        val isMigrating: Boolean = false,
        val isMigrated: Boolean = false,
        val hasFailure: Boolean = false,
    )
}

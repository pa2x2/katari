package app.katari.sourcemigration.dialog

import androidx.lifecycle.ViewModel
import app.katari.entry.interactions.migration.EntryMigrationExecuteIntent
import app.katari.entry.interactions.migration.EntryMigrationExecutionResult
import app.katari.entry.interactions.migration.EntryMigrationFeature
import app.katari.entry.interactions.migration.EntryMigrationMode
import app.katari.entry.interactions.migration.EntryMigrationOption
import app.katari.entry.interactions.migration.EntryMigrationPreparationResult
import app.katari.entry.interactions.migration.EntryMigrationPrepareIntent
import app.katari.entry.interactions.migration.EntryMigrationReference
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.service.SourcePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import tachiyomi.domain.entry.model.Entry

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class MigrateEntryDialogViewModel(
    private val sourcePreference: SourcePreferences,
    private val migration: EntryMigrationFeature,
) : ViewModel() {

    val state: StateFlow<MigrateEntryDialogViewModel.State>
        field = MutableStateFlow<MigrateEntryDialogViewModel.State>(State())

    private var source: Entry? = null
    private var target: Entry? = null

    suspend fun init(current: Entry, target: Entry) {
        source = current
        this.target = target
        state.update { State(isLoading = true) }
        when (val result = migration.prepare(EntryMigrationPrepareIntent(current, target))) {
            is EntryMigrationPreparationResult.Ready -> {
                val removeDownloads = sourcePreference.migrationRemoveDownloads.get()
                state.update {
                    State(
                        reference = result.reference,
                        availableOptions = result.availableOptions.toList(),
                        selectedOptions = result.availableOptions.filterTo(mutableSetOf()) {
                            it != EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS || removeDownloads
                        },
                        isLoading = false,
                    )
                }
            }
            is EntryMigrationPreparationResult.OperationalFailure,
            is EntryMigrationPreparationResult.Rejected,
            -> state.update { State(isLoading = false, hasFailure = true) }
        }
    }

    fun toggleSelection(option: EntryMigrationOption) {
        state.update {
            val selectedOptions = it.selectedOptions.toMutableSet()
                .apply { if (contains(option)) remove(option) else add(option) }
                .toSet()
            it.copy(selectedOptions = selectedOptions)
        }
    }

    suspend fun migrateEntry(mode: EntryMigrationMode): Boolean {
        val current = state.value
        val reference = current.reference ?: return false
        if (EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS in current.availableOptions) {
            sourcePreference.migrationRemoveDownloads.set(
                EntryMigrationOption.REMOVE_SOURCE_DOWNLOADS in current.selectedOptions,
            )
        }
        state.update { it.copy(isMigrating = true, hasFailure = false) }
        return when (
            migration.execute(
                EntryMigrationExecuteIntent(
                    reference = reference,
                    mode = mode,
                    selectedOptions = current.selectedOptions,
                ),
            )
        ) {
            is EntryMigrationExecutionResult.Applied -> {
                state.update { it.copy(isMigrating = false, isMigrated = true) }
                true
            }
            EntryMigrationExecutionResult.Conflict -> {
                val source = source
                val target = target
                if (source != null && target != null) {
                    init(source, target)
                } else {
                    state.update { it.copy(isMigrating = false, hasFailure = true) }
                }
                false
            }
            is EntryMigrationExecutionResult.OperationalFailure,
            is EntryMigrationExecutionResult.Rejected,
            -> {
                state.update { it.copy(isMigrating = false, hasFailure = true) }
                false
            }
        }
    }

    data class State(
        val reference: EntryMigrationReference? = null,
        val availableOptions: List<EntryMigrationOption> = emptyList(),
        val selectedOptions: Set<EntryMigrationOption> = emptySet(),
        val isLoading: Boolean = true,
        val isMigrating: Boolean = false,
        val isMigrated: Boolean = false,
        val hasFailure: Boolean = false,
    )
}

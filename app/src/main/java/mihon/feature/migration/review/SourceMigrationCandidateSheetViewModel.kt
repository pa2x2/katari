package mihon.feature.migration.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationWorkScheduler
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.service.SourceManager

@AssistedInject
internal class SourceMigrationCandidateSheetViewModel(
    @Assisted private val sessionId: SourceMigrationSessionId,
    @Assisted private val sourceEntryId: Long,
    private val sessionStore: SourceMigrationSessionStore,
    private val targetSelector: SourceMigrationTargetSelector,
    private val candidateEntryResolver: SourceMigrationCandidateEntryResolver,
    private val workScheduler: SourceMigrationWorkScheduler,
    private val sourceManager: SourceManager,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sessionId: SourceMigrationSessionId, sourceEntryId: Long): SourceMigrationCandidateSheetViewModel
    }

    val state: StateFlow<SourceMigrationCandidateSheetState>
        field = MutableStateFlow<SourceMigrationCandidateSheetState>(SourceMigrationCandidateSheetState())

    private val eventFlow = MutableSharedFlow<SourceMigrationCandidateSheetEvent>(extraBufferCapacity = 1)
    val events = eventFlow.asSharedFlow()

    init {
        viewModelScope.launchIO {
            sessionStore.observeCandidates(sessionId, sourceEntryId).collectLatest { candidates ->
                state.update { state ->
                    state.copy(
                        isLoaded = true,
                        candidates = candidates.map { candidate ->
                            SourceMigrationCandidateItem(
                                candidate = candidate,
                                sourceName = sourceManager.getDisplayInfo(candidate.targetSourceId).name,
                            )
                        },
                    )
                }
            }
        }
    }

    fun select(candidate: SourceMigrationCandidate) {
        if (state.value.isWorking) return
        state.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launchIO {
            val result = targetSelector.select(sessionId, sourceEntryId, candidate)
            val selected = result == SourceMigrationTargetSelectionResult.Selected
            state.update {
                it.copy(
                    isWorking = false,
                    failed = !selected,
                )
            }
            if (selected) eventFlow.emit(SourceMigrationCandidateSheetEvent.Dismiss)
        }
    }

    fun clearSelection() {
        if (state.value.isWorking) return
        state.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launchIO {
            val result = targetSelector.clear(sessionId, sourceEntryId)
            val cleared = result == SourceMigrationTargetSelectionResult.Cleared
            state.update {
                it.copy(
                    isWorking = false,
                    failed = !cleared,
                )
            }
            if (cleared) eventFlow.emit(SourceMigrationCandidateSheetEvent.Dismiss)
        }
    }

    fun openDetails(candidate: SourceMigrationCandidate) {
        if (state.value.isWorking) return
        state.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launchIO {
            val session = sessionStore.get(sessionId)
            val entryId = session
                ?.let { candidateEntryResolver.resolve(it, sourceEntryId, candidate) }
                ?.target
                ?.id
            state.update {
                it.copy(
                    isWorking = false,
                    detailsEntryId = entryId,
                    failed = entryId == null,
                )
            }
        }
    }

    fun consumeDetailsEntry() {
        state.update { it.copy(detailsEntryId = null) }
    }

    fun searchAgain() {
        if (state.value.isWorking) return
        state.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launchIO {
            val started = workScheduler.restartItemDiscovery(
                sessionId = sessionId,
                sourceEntryId = sourceEntryId,
                depth = SourceMigrationDiscoveryDepth.BROAD,
            )
            state.update {
                it.copy(
                    isWorking = false,
                    failed = !started,
                )
            }
            if (started) eventFlow.emit(SourceMigrationCandidateSheetEvent.Dismiss)
        }
    }
}

internal data class SourceMigrationCandidateSheetState(
    val isLoaded: Boolean = false,
    val candidates: List<SourceMigrationCandidateItem> = emptyList(),
    val isWorking: Boolean = false,
    val failed: Boolean = false,
    val detailsEntryId: Long? = null,
)

internal data class SourceMigrationCandidateItem(
    val candidate: SourceMigrationCandidate,
    val sourceName: String,
)

internal enum class SourceMigrationCandidateSheetEvent {
    Dismiss,
}

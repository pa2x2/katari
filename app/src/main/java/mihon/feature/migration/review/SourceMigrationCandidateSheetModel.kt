package mihon.feature.migration.review

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationCandidate
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.work.SourceMigrationWorkScheduler
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class SourceMigrationCandidateSheetModel(
    private val sessionId: SourceMigrationSessionId,
    private val sourceEntryId: Long,
    private val sessionStore: SourceMigrationSessionStore = Injekt.get(),
    private val targetSelector: SourceMigrationTargetSelector = Injekt.get(),
    private val workScheduler: SourceMigrationWorkScheduler = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<SourceMigrationCandidateSheetState>(SourceMigrationCandidateSheetState()) {

    init {
        screenModelScope.launchIO {
            sessionStore.observeCandidates(sessionId, sourceEntryId).collectLatest { candidates ->
                mutableState.update { state ->
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
        mutableState.update { it.copy(isWorking = true, failed = false) }
        screenModelScope.launchIO {
            val result = targetSelector.select(sessionId, sourceEntryId, candidate)
            mutableState.update {
                it.copy(
                    isWorking = false,
                    completed = result == SourceMigrationTargetSelectionResult.Selected,
                    failed = result != SourceMigrationTargetSelectionResult.Selected,
                )
            }
        }
    }

    fun searchAgain() {
        if (state.value.isWorking) return
        mutableState.update { it.copy(isWorking = true, failed = false) }
        screenModelScope.launchIO {
            val started = workScheduler.restartItemDiscovery(
                sessionId = sessionId,
                sourceEntryId = sourceEntryId,
                depth = SourceMigrationDiscoveryDepth.BROAD,
            )
            mutableState.update {
                it.copy(
                    isWorking = false,
                    completed = started,
                    failed = !started,
                )
            }
        }
    }
}

internal data class SourceMigrationCandidateSheetState(
    val isLoaded: Boolean = false,
    val candidates: List<SourceMigrationCandidateItem> = emptyList(),
    val isWorking: Boolean = false,
    val completed: Boolean = false,
    val failed: Boolean = false,
)

internal data class SourceMigrationCandidateItem(
    val candidate: SourceMigrationCandidate,
    val sourceName: String,
)

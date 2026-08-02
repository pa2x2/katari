package mihon.feature.migration.review

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.migration.session.model.SourceMigrationDiscoveryDepth
import mihon.feature.migration.session.model.SourceMigrationSession
import mihon.feature.migration.session.model.SourceMigrationSessionId
import mihon.feature.migration.session.model.SourceMigrationSessionStage
import mihon.feature.migration.work.SourceMigrationWorkScheduler
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class SourceMigrationReviewScreenModel(
    private val sessionId: SourceMigrationSessionId,
    private val sessionStore: SourceMigrationSessionStore = Injekt.get(),
    private val workScheduler: SourceMigrationWorkScheduler = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<SourceMigrationReviewState>(SourceMigrationReviewState()) {

    init {
        screenModelScope.launchIO {
            sessionStore.observe(sessionId).collectLatest { session ->
                mutableState.update { state ->
                    val discoveryActive = session?.stage in DISCOVERY_STAGES
                    val filter = when {
                        discoveryActive && state.filter !in DISCOVERY_FILTERS -> SourceMigrationReviewFilter.ALL
                        !discoveryActive && state.filter in DISCOVERY_ONLY_FILTERS -> SourceMigrationReviewFilter.ALL
                        else -> state.filter
                    }
                    state.copy(
                        isLoaded = true,
                        session = session,
                        originSourceName = session?.let { sourceName(it.originSourceId) }.orEmpty(),
                        groups = session?.toReviewGroups().orEmpty(),
                        filter = filter,
                    )
                }
            }
        }
    }

    fun setFilter(filter: SourceMigrationReviewFilter) {
        mutableState.update { it.copy(filter = filter) }
    }

    fun setIncluded(sourceEntryId: Long, included: Boolean) {
        screenModelScope.launchIO {
            sessionStore.setItemIncluded(sessionId, sourceEntryId, included)
        }
    }

    fun toggleGroup(groupId: Long) {
        val group = state.value.groups.firstOrNull { it.id == groupId } ?: return
        val included = !group.allReadyMappingsIncluded
        screenModelScope.launchIO {
            group.readyMappings.forEach { mapping ->
                sessionStore.setItemIncluded(sessionId, mapping.item.sourceEntryId, included)
            }
        }
    }

    fun startExecution() {
        if (state.value.actionInProgress) return
        mutableState.update { it.copy(actionInProgress = true) }
        screenModelScope.launchIO {
            workScheduler.startExecution(sessionId)
            mutableState.update { it.copy(actionInProgress = false) }
        }
    }

    fun pauseDiscovery() {
        screenModelScope.launchIO { workScheduler.pauseDiscovery(sessionId) }
    }

    fun startDiscovery() {
        screenModelScope.launchIO { workScheduler.startDiscovery(sessionId) }
    }

    fun resumeDiscovery() {
        screenModelScope.launchIO { workScheduler.startDiscovery(sessionId) }
    }

    fun discardPreparation() {
        if (state.value.actionInProgress) return
        mutableState.update { it.copy(actionInProgress = true) }
        screenModelScope.launchIO {
            val discarded = withContext(NonCancellable) {
                runCatching { workScheduler.discardPreparation(sessionId) }.getOrDefault(false)
            }
            mutableState.update {
                it.copy(
                    actionInProgress = false,
                    discardCompleted = discarded,
                )
            }
        }
    }

    fun retryUnresolved() {
        if (state.value.actionInProgress || state.value.unresolvedCount == 0) return
        mutableState.update { it.copy(actionInProgress = true) }
        screenModelScope.launchIO {
            workScheduler.restartUnresolvedDiscovery(
                sessionId = sessionId,
                depth = SourceMigrationDiscoveryDepth.BROAD,
            )
            mutableState.update { it.copy(actionInProgress = false) }
        }
    }

    fun pauseExecution() {
        screenModelScope.launchIO { workScheduler.pauseExecution(sessionId) }
    }

    fun resumeExecution() {
        screenModelScope.launchIO { workScheduler.startExecution(sessionId) }
    }

    private fun SourceMigrationSession.toReviewGroups(): List<SourceMigrationReviewGroup> {
        val itemByEntryId = items.associateBy { item -> item.sourceEntryId }
        return groups.map { group ->
            SourceMigrationReviewGroup(
                id = group.groupId,
                visibleEntryId = group.visibleEntryId,
                title = group.visibleTitle,
                memberCount = group.members.size,
                mappings = group.members
                    .filter { member -> member.selected }
                    .mapNotNull { member -> itemByEntryId[member.entryId] }
                    .map { item ->
                        SourceMigrationReviewMapping(
                            item = item,
                            sourceName = sourceName(item.sourceId),
                            targetSourceName = item.targetSourceId?.let(::sourceName),
                        )
                    },
                notSelectedMembers = group.members
                    .filterNot { member -> member.selected }
                    .map { member ->
                        SourceMigrationReviewMember(
                            member = member,
                            sourceName = sourceName(member.sourceId),
                        )
                    },
            )
        }
    }

    private fun sourceName(sourceId: Long): String = sourceManager.getDisplayInfo(sourceId).name

    private companion object {
        val DISCOVERY_STAGES = setOf(
            SourceMigrationSessionStage.DISCOVERY_QUEUED,
            SourceMigrationSessionStage.DISCOVERING,
            SourceMigrationSessionStage.DISCOVERY_PAUSED,
        )
        val DISCOVERY_FILTERS = setOf(
            SourceMigrationReviewFilter.ALL,
            SourceMigrationReviewFilter.READY,
            SourceMigrationReviewFilter.FOUND,
            SourceMigrationReviewFilter.SEARCHING,
            SourceMigrationReviewFilter.NO_MATCH,
        )
        val DISCOVERY_ONLY_FILTERS = setOf(
            SourceMigrationReviewFilter.FOUND,
            SourceMigrationReviewFilter.SEARCHING,
        )
    }
}

package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionFailurePolicy
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureExecutionPointId
import mihon.feature.graph.afterCommitVolatileFeatureExecutionPointDefinition
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal val ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT =
    afterCommitVolatileFeatureExecutionPointDefinition<EntryLibraryUpdateNewChildrenEvent>(
        id = FeatureExecutionPointId("entry.library-update-refresh.new-children"),
        owner = ENTRY_LIBRARY_UPDATE_REFRESH_OWNER,
        failurePolicy = FeatureExecutionFailurePolicy.FAIL_FAST,
    )

internal data class EntryLibraryUpdateNewChildrenEvent(
    val entry: Entry,
    val newChildren: List<EntryChapter>,
    val session: EntryLibraryUpdateExecutionSession,
)

/**
 * Process-local state contributed by execution participants for one Library update run.
 *
 * The Library coordinator only completes opaque participant-owned state. It never names a participant or interprets
 * its value.
 */
internal class EntryLibraryUpdateExecutionSession {
    private val completed = AtomicBoolean(false)
    private val states = ConcurrentHashMap<FeatureExecutionParticipantId, ParticipantState>()

    @Suppress("UNCHECKED_CAST")
    fun <S : Any> state(
        participant: FeatureExecutionParticipantId,
        create: () -> S,
        complete: (S) -> Unit,
    ): S {
        check(!completed.get()) { "Cannot contribute work after the Library update session completed" }
        val state = states.computeIfAbsent(participant) {
            val value = create()
            ParticipantState(value) { complete(it as S) }
        }
        return state.value as S
    }

    fun complete() {
        if (!completed.compareAndSet(false, true)) return
        states.entries
            .sortedBy { it.key.value }
            .forEach { (_, state) -> state.complete(state.value) }
        states.clear()
    }

    private data class ParticipantState(
        val value: Any,
        val complete: (Any) -> Unit,
    )
}

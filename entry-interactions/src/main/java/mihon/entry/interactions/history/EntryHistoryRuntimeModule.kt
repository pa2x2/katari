package mihon.entry.interactions.history

import mihon.entry.interactions.media.session.EntryMediaSessionConsequence
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.execution.FeatureExecutionContextResolver
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryHistoryFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.history",
    contributor = EntryHistoryFeatureContributor,
) {
    addSingletonFactory<EntryHistoryFeature> { DefaultEntryHistoryFeature(get()) }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            entryHistoryMediaSessionBinding { get<EntryHistoryFeature>() },
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryHistoryFeature>() }),
    )
}

internal fun entryHistoryMediaSessionBinding(
    feature: () -> EntryHistoryFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_HISTORY_MEDIA_SESSION_PARTICIPANT,
    contextResolver = FeatureExecutionContextResolver { execution ->
        listOf(
            contextEvidence(
                ENTRY_MEDIA_SESSION_HISTORY_ALLOWED,
                execution.permits(EntryMediaSessionConsequence.RECORD_HISTORY),
            ),
        )
    },
    handler = FeatureExecutionHandler { execution ->
        when (val event = execution.event) {
            is EntryMediaSessionEvent.Progressed -> {
                event.activity?.let { feature().record(event, it) }
                execution.progressResult
                    ?.takeIf { it.completedNow }
                    ?.let { feature().recordCompletion(event, it.state) }
            }
            is EntryMediaSessionEvent.ActivityRecorded ->
                feature().record(event, event.activity)
        }
    },
)

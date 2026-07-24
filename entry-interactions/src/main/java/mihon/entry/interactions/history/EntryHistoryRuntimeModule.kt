package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionContextResolver
import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.contextEvidence
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
            is EntryMediaSessionEvent.Progressed ->
                event.activity?.let { feature().record(event, it) }
            is EntryMediaSessionEvent.ActivityRecorded ->
                feature().record(event, event.activity)
        }
    },
)

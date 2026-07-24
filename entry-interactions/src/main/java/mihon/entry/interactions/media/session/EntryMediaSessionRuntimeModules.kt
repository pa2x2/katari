package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryMediaSessionFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.media-session",
    contributor = EntryMediaSessionFeatureContributor,
) {
    addSingletonFactory<EntryMediaSessionEventSink> {
        EntryMediaSessionEventSink { event -> Injekt.get<EntryMediaSessionFeature>().onEvent(event) }
    }
    addSingletonFactory<EntryMediaSessionFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryMediaSessionFeature(
            evaluation = composition.featureGraphEvaluation,
            executions = composition.featureExecutions,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryMediaSessionFeature>() }),
    )
}

internal val EntryMediaSessionIncognitoFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.media-session.incognito",
    contributor = EntryMediaSessionIncognitoContributor,
) { context ->
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            entryMediaSessionIncognitoBinding(
                repository = { get<EntryRepository>() },
                incognitoState = context.dependencies.mediaSessionIncognitoState,
            ),
        ),
    )
}

internal fun entryMediaSessionIncognitoBinding(
    repository: () -> EntryRepository,
    incognitoState: EntryMediaSessionIncognitoState,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_MEDIA_SESSION_INCOGNITO_PARTICIPANT,
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event
        val owner = repository().getEntryById(event.child.entryId)
        val sourceId = owner?.source ?: event.visibleEntry.source
        if (incognitoState.isIncognito(sourceId)) {
            execution.block(ENTRY_MEDIA_SESSION_RECORDING_CONSEQUENCES)
        }
    },
)

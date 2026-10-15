package mihon.entry.interactions.history

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.media.session.EntryMediaSessionConsequence
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.execution.FeatureExecutionContextResolver
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
import tachiyomi.domain.history.repository.HistoryRepository

@ContributesTo(AppScope::class)
@BindingContainer
object EntryHistoryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun feature(repository: HistoryRepository): EntryHistoryFeature = DefaultEntryHistoryFeature(repository)

    @Provides
    @IntoSet
    fun graphContributor(): FeatureGraphContributor = EntryHistoryFeatureContributor

    @Provides
    @IntoSet
    fun mediaSessionParticipant(feature: () -> EntryHistoryFeature): FeatureExecutionParticipantBinding<*> =
        entryHistoryMediaSessionBinding(feature)

    @Provides
    @IntoSet
    fun exportFeature(feature: () -> EntryHistoryFeature): InjektExport<*> = injektExport(feature)
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

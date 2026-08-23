package mihon.entry.interactions.history

import io.mockk.coVerify
import io.mockk.mockk
import mihon.entry.interactions.media.EntryMediaSessionCapability
import mihon.entry.interactions.media.session.EntryMediaSessionActivity
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionExecutionEvent
import mihon.entry.interactions.media.session.mediaSessionContractEvent
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractScenario
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.history.repository.HistoryRepository

class EntryHistoryContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryHistoryFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        val featureReference = FeatureContractReference(ENTRY_HISTORY_FEATURE_ID, EntryHistoryBehaviorContract)
        sink.add(
            FeatureContractVerifier(featureReference) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(
                        input.provider(EntryMediaSessionCapability.definition).type,
                    )
                    val repository = mockk<HistoryRepository>(relaxed = true)
                    DefaultEntryHistoryFeature(repository).record(event, requireNotNull(event.activity))
                    coVerify(exactly = 1) { repository.recordActivity(any()) }
                }
            },
        )
        val executionReference = FeatureExecutionContractReference(
            ENTRY_HISTORY_MEDIA_SESSION_PARTICIPANT.id,
            EntryHistoryBehaviorContract,
        )
        sink.add(
            FeatureExecutionContractVerifier(executionReference) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(
                        input.provider(EntryMediaSessionCapability.definition).type,
                    )
                    var recorded: EntryMediaSessionActivity? = null
                    val feature = object : EntryHistoryFeature {
                        override suspend fun record(
                            event: EntryMediaSessionEvent,
                            activity: EntryMediaSessionActivity,
                        ) {
                            recorded = activity
                        }

                        override suspend fun recordCompletion(
                            event: EntryMediaSessionEvent.Progressed,
                            progress: EntryProgressState,
                        ) = Unit

                        override suspend fun recordManualCompletions(
                            entry: tachiyomi.domain.entry.model.Entry,
                            children: List<tachiyomi.domain.entry.model.EntryChapter>,
                        ) = Unit
                    }
                    entryHistoryMediaSessionBinding { feature }
                        .handler
                        .execute(EntryMediaSessionExecutionEvent(event))
                    contractExpectation(
                        recorded == event.activity,
                        "History must record activity contributed by a Media Session event",
                    )
                }
            },
        )
        sink.add(
            FeatureExecutionContractScenario(
                FeatureContractScenarioId("entry.history.media-session.execution.applicable"),
                executionReference,
            ) { listOf(contextEvidence(ENTRY_MEDIA_SESSION_HISTORY_ALLOWED, true)) },
        )
    }
}

package mihon.entry.interactions.translate.ahead

import mihon.entry.interactions.media.session.EntryMediaSessionExecutionEvent
import mihon.entry.interactions.media.session.mediaSessionContractEvent
import mihon.entry.interactions.translate.EntryTranslateCapability
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor

class EntryTranslateAheadContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTranslateAheadContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureExecutionContractVerifier(
                FeatureExecutionContractReference(
                    ENTRY_TRANSLATE_AHEAD_MEDIA_SESSION_PARTICIPANT.id,
                    EntryTranslateAheadBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val event = mediaSessionContractEvent(input.provider(EntryTranslateCapability.definition).type)
                    val received = mutableListOf<Double>()
                    val trigger = EntryTranslateAheadTrigger { _, _, fraction, _ -> received += fraction }

                    entryTranslateAheadMediaSessionBinding { trigger }
                        .handler
                        .execute(EntryMediaSessionExecutionEvent(event))

                    contractExpectation(
                        received == listOfNotNull(event.fraction),
                        "Translate ahead must receive how far the reader got through the chapter",
                    )
                }
            },
        )
    }
}

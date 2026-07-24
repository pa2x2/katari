package mihon.entry.interactions

import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor

class EntryMediaSessionContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryMediaSessionFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_MEDIA_SESSION_FEATURE_ID, EntryMediaSessionBehaviorContract),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryMediaSessionCapability.definition)
                    val progressEvent = mediaSessionContractEvent(provider.type)
                    val event = EntryMediaSessionEvent.ActivityRecorded(
                        visibleEntry = progressEvent.visibleEntry,
                        child = progressEvent.child,
                        activity = requireNotNull(progressEvent.activity),
                    )
                    contractExpectation(
                        provider.onEvent(event) == EntryMediaSessionResult.Handled,
                        "A Media Session provider must operationally emit structured session events",
                    )
                }
            },
        )
    }
}

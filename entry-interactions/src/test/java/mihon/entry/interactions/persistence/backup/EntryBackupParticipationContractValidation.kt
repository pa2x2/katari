package mihon.entry.interactions

import kotlinx.serialization.KSerializer
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.validation.FeatureExecutionContractReference
import mihon.feature.graph.validation.FeatureExecutionContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink

internal fun <T> FeatureValidationContributionSink.addEntryBackupParticipationContract(
    participant: FeatureExecutionParticipantDefinition<*>,
    contract: FeatureBehaviorContract,
    serializer: KSerializer<T>,
    example: T,
) {
    add(
        FeatureExecutionContractVerifier(
            FeatureExecutionContractReference(participant.id, contract),
        ) {
            verifyFeatureContract {
                val payload = EntryBackupStateCodec.encode(serializer, example)
                contractExpectation(
                    EntryBackupStateCodec.decode(serializer, payload) == example,
                    "Feature-owned backup state must survive its portable codec",
                )
            }
        },
    )
}

internal fun <T> FeatureValidationContributionSink.addEntryBackupParticipationContractForSubject(
    participant: FeatureExecutionParticipantDefinition<*>,
    contract: FeatureBehaviorContract,
    serializer: KSerializer<T>,
    example: (ContentTypeId) -> T,
) {
    add(
        FeatureExecutionContractVerifier(
            FeatureExecutionContractReference(participant.id, contract),
        ) { input ->
            verifyFeatureContract {
                val value = example(input.subject.contentType)
                val payload = EntryBackupStateCodec.encode(serializer, value)
                contractExpectation(
                    EntryBackupStateCodec.decode(serializer, payload) == value,
                    "Feature-owned backup state must survive its portable codec",
                )
            }
        },
    )
}

package mihon.feature.graph.validation

import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureId

class ServiceLoadedValidationContributor : FeatureValidationContributor {
    override val owner = ContributionOwner("service.feature")

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                contract = FeatureContractReference(
                    FeatureId("service.feature"),
                    ServiceLoadedValidationContract,
                ),
                verification = FeatureContractVerification { FeatureContractVerificationResult.Passed },
            ),
        )
    }
}

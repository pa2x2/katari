package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry

class EntryUpdateEligibilityContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryUpdateEligibilityFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID,
                    EntryUpdateEligibilityBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    val policy = EntryUpdateEligibilityPolicy(false, false, false, false)
                    val feature = eligibilityFeature(type, policy)
                    contractExpectation(
                        feature.evaluate(unrestrictedRequest(type)) == EntryUpdateEligibility.Eligible,
                        "Update eligibility must accept an unrestricted entry",
                    )
                }
            },
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID,
                    EntryUpdateEligibilityDecisionBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val policy = input.evidence(ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT)
                    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
                    contractExpectation(
                        !policy.skipCompleted &&
                            !policy.skipWhenUnconsumed &&
                            !policy.skipWhenNotStarted &&
                            !policy.skipOutsideReleasePeriod,
                        "Update eligibility applicable scenario must disable every optional restriction",
                    )
                    contractExpectation(
                        eligibilityFeature(type, policy).evaluate(unrestrictedRequest(type)) ==
                            EntryUpdateEligibility.Eligible,
                        "Update eligibility applicable scenario must execute the eligible decision",
                    )
                }
            },
        )
        sink.add(
            FeatureContractScenario(
                FeatureContractScenarioId("entry.update-eligibility.decision.applicable"),
                FeatureContractReference(
                    ENTRY_UPDATE_ELIGIBILITY_FEATURE_ID,
                    EntryUpdateEligibilityDecisionBehaviorContract,
                ),
                ENTRY_UPDATE_ELIGIBILITY_DECISION_INTEGRATION,
            ) {
                listOf(
                    contextEvidence(
                        ENTRY_UPDATE_ELIGIBILITY_POLICY_CONTEXT,
                        EntryUpdateEligibilityPolicy(false, false, false, false),
                    ),
                    contextEvidence(ENTRY_UPDATE_ELIGIBILITY_ONE_SHOT_CONTEXT, false),
                    contextEvidence(ENTRY_UPDATE_ELIGIBILITY_COMPLETED_CONTEXT, false),
                    contextEvidence(ENTRY_UPDATE_ELIGIBILITY_UNCONSUMED_CONTEXT, false),
                    contextEvidence(ENTRY_UPDATE_ELIGIBILITY_NOT_STARTED_CONTEXT, false),
                    contextEvidence(ENTRY_UPDATE_ELIGIBILITY_RELEASE_PERIOD_CONTEXT, false),
                )
            },
        )
    }

    private fun eligibilityFeature(
        type: EntryType,
        policy: EntryUpdateEligibilityPolicy,
    ): EntryUpdateEligibilityFeature = DefaultEntryUpdateEligibilityFeature(
        evaluation = productionSubjectEvaluation(type, EntryUpdateEligibilityFeatureContributor),
        currentPolicy = { policy },
    )

    private fun unrestrictedRequest(type: EntryType): EntryUpdateEligibilityRequest = EntryUpdateEligibilityRequest(
        entry = Entry.create().copy(type = type),
        totalCount = null,
        unconsumedCount = null,
        hasStarted = null,
    )
}

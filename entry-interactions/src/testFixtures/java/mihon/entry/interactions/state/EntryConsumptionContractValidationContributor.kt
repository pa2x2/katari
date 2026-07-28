package mihon.entry.interactions

import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureContractScenarioId
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractScenario
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryConsumptionContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryConsumptionFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_CONSUMPTION_FEATURE_ID, EntryConsumptionProviderBehaviorContract),
            ) { input -> verifyConsumptionCoordinator(input) },
        )
        addBooleanScenario(
            sink,
            EntryConsumptionEligibilityBehaviorContract,
            ENTRY_CONSUMPTION_ELIGIBILITY_INTEGRATION,
            FeatureContractScenarioId("entry.consumption.eligibility.applicable"),
            ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT,
        )
        addBooleanScenario(
            sink,
            EntryConsumptionMutationBehaviorContract,
            ENTRY_CONSUMPTION_MUTATION_RESULT_INTEGRATION,
            FeatureContractScenarioId("entry.consumption.mutation.applicable"),
            ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT,
        )
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_CONSUMPTION_FEATURE_ID, EntryConsumptionLifecycleBehaviorContract),
            ) { input -> verifyConsumptionCoordinator(input) },
        )
        sink.add(
            FeatureContractScenario(
                id = FeatureContractScenarioId("entry.consumption.lifecycle.applicable"),
                contract = FeatureContractReference(
                    ENTRY_CONSUMPTION_FEATURE_ID,
                    EntryConsumptionLifecycleBehaviorContract,
                ),
                integration = ENTRY_CONSUMPTION_LIFECYCLE_INTEGRATION,
            ) {
                listOf(
                    contextEvidence(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT, true),
                    contextEvidence(ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT, true),
                )
            },
        )
    }

    private fun addBooleanScenario(
        sink: FeatureValidationContributionSink,
        contract: FeatureBehaviorContract,
        integration: FeatureIntegrationId,
        scenario: FeatureContractScenarioId,
        context: ContextInputDefinition<Boolean>,
    ) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_CONSUMPTION_FEATURE_ID, contract),
            ) { input -> verifyConsumptionCoordinator(input) },
        )
        sink.add(
            FeatureContractScenario(
                id = scenario,
                contract = FeatureContractReference(ENTRY_CONSUMPTION_FEATURE_ID, contract),
                integration = integration,
            ) { listOf(contextEvidence(context, true)) },
        )
    }
}

private suspend fun verifyConsumptionCoordinator(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val provider = input.provider(EntryConsumptionCapability.definition)
    val evaluation = productionSubjectEvaluation(
        EntryConsumptionCapability.bind(provider),
        EntryConsumptionFeatureContributor,
    )
    val entry = Entry.create().copy(id = 51L, type = provider.type)
    val child = EntryChapter.create().copy(id = 81L, read = false)
    val lifecycleEvents = mutableListOf<EntryDownloadLifecycleEvent>()
    val feature = DefaultEntryConsumptionFeature(
        evaluation = evaluation,
        interaction = object : EntryConsumptionInteraction {
            override suspend fun setConsumed(
                entry: Entry,
                chapters: List<EntryChapter>,
                consumed: Boolean,
            ): List<EntryChapter> = chapters
        },
        downloadLifecycle = EntryDownloadLifecycleEventSink { event ->
            lifecycleEvents += event
            EntryDownloadLifecycleResult.Handled
        },
    )

    contractExpectation(feature.isApplicable(provider.type), "Consumption must be applicable")
    contractExpectation(
        feature.canSetConsumed(provider.type, EntryConsumptionStatus(false, false), consumed = true),
        "Consumption must expose a real state change",
    )
    contractExpectation(
        feature.setConsumed(entry, listOf(child), consumed = true) == EntryConsumptionResult.Changed(listOf(child)),
        "Consumption must return changed children",
    )
    contractExpectation(
        lifecycleEvents.singleOrNull() == EntryDownloadLifecycleEvent.MarkedConsumed(entry, listOf(child)),
        "Consumption must emit the marked-consumed lifecycle event",
    )
}

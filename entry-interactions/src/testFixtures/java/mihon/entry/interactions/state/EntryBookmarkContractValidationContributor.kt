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

class EntryBookmarkContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryBookmarkFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_BOOKMARK_FEATURE_ID, EntryBookmarkProviderBehaviorContract),
            ) { input -> verifyBookmarkCoordinator(input) },
        )
        addContextContract(
            sink,
            EntryBookmarkAvailabilityBehaviorContract,
            ENTRY_BOOKMARK_AVAILABILITY_INTEGRATION,
            FeatureContractScenarioId("entry.bookmarking.availability.applicable"),
            ENTRY_BOOKMARK_SELECTION_CHANGE_CONTEXT,
        )
        addContextContract(
            sink,
            EntryBookmarkMutationBehaviorContract,
            ENTRY_BOOKMARK_MUTATION_INTEGRATION,
            FeatureContractScenarioId("entry.bookmarking.mutation.applicable"),
            ENTRY_BOOKMARK_MUTATION_CHANGE_CONTEXT,
        )
    }

    private fun addContextContract(
        sink: FeatureValidationContributionSink,
        contract: FeatureBehaviorContract,
        integration: FeatureIntegrationId,
        scenario: FeatureContractScenarioId,
        context: ContextInputDefinition<Boolean>,
    ) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(ENTRY_BOOKMARK_FEATURE_ID, contract),
            ) { input -> verifyBookmarkCoordinator(input) },
        )
        sink.add(
            FeatureContractScenario(
                scenario,
                FeatureContractReference(ENTRY_BOOKMARK_FEATURE_ID, contract),
                integration,
            ) { listOf(contextEvidence(context, true)) },
        )
    }
}

private suspend fun verifyBookmarkCoordinator(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val provider = input.provider(EntryBookmarkCapability.definition)
    val evaluation = productionSubjectEvaluation(
        EntryBookmarkCapability.bind(provider),
        EntryBookmarkFeatureContributor,
    )
    val entry = Entry.create().copy(id = 52L, type = provider.type)
    val child = EntryChapter.create().copy(id = 82L, bookmark = false)
    val mutations = mutableListOf<List<Long>>()
    val feature = DefaultEntryBookmarkFeature(
        evaluation = evaluation,
        interaction = object : EntryBookmarkInteraction {
            override suspend fun setBookmarked(
                entry: Entry,
                chapters: List<EntryChapter>,
                bookmarked: Boolean,
            ) {
                mutations += chapters.map(EntryChapter::id)
            }
        },
    )

    contractExpectation(feature.isApplicable(provider.type), "Bookmarking must be applicable")
    contractExpectation(
        feature.availability(
            EntryBookmarkTarget(provider.type, EntryBookmarkStatus(false)),
            bookmarked = true,
        ) == EntryBookmarkAvailability.Available,
        "Bookmarking must expose a state-changing selection",
    )
    contractExpectation(
        feature.setBookmarked(entry, listOf(child), bookmarked = true) == EntryBookmarkMutationResult.Applied(1),
        "Bookmarking must report the applied mutation",
    )
    contractExpectation(mutations == listOf(listOf(child.id)), "Bookmarking dispatched wrong children")
}

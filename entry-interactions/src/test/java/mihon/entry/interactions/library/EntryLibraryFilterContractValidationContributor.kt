package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
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
import tachiyomi.core.common.preference.TriState

class EntryLibraryFilterContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryFilterFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        libraryFilterContracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_LIBRARY_FILTER_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference, ::verifyLibraryFilter))
            if (item.integration == ENTRY_LIBRARY_FILTER_CONTEXT_INTEGRATION) {
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("entry.library-filtering.context.applicable"),
                        reference,
                        item.integration,
                    ) {
                        listOf(
                            contextEvidence(ENTRY_LIBRARY_FILTER_POLICY_CONTEXT, neutralLibraryFilterPolicy),
                            contextEvidence(ENTRY_LIBRARY_FILTER_STATE_CONTEXT, neutralLibraryFilterState),
                            contextEvidence(ENTRY_LIBRARY_FILTER_TRACKING_CONTEXT, neutralLibraryTrackingState),
                        )
                    },
                )
            }
        }
    }
}

private data class LibraryFilterContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
)

private val libraryFilterContracts = listOf(
    LibraryFilterContract(ENTRY_LIBRARY_FILTER_PARTICIPATION_INTEGRATION, EntryLibraryFilterBehaviorContract),
    LibraryFilterContract(ENTRY_LIBRARY_FILTER_CONTEXT_INTEGRATION, EntryLibraryFilterContextBehaviorContract),
    LibraryFilterContract(ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION, EntryLibraryFilterProgressBehaviorContract),
    LibraryFilterContract(ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION, EntryLibraryFilterBookmarkBehaviorContract),
    LibraryFilterContract(
        ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION,
        EntryLibraryFilterReleasePeriodBehaviorContract,
    ),
)

private suspend fun verifyLibraryFilter(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val type = EntryType.entries.single { it.toContentTypeId() == input.subject.contentType }
    val bindings = when (input.subject.integration) {
        ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION -> listOf(
            EntryLibraryProgressCapability.bind(input.provider(EntryLibraryProgressCapability.definition)),
        )
        ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION -> listOf(
            EntryLibraryProgressCapability.bind(input.provider(EntryLibraryProgressCapability.definition)),
            EntryBookmarkCapability.bind(input.provider(EntryBookmarkCapability.definition)),
        )
        ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION -> listOf(
            EntryOutsideReleasePeriodFilterCapability.bind(
                input.provider(EntryOutsideReleasePeriodFilterCapability.definition),
            ),
        )
        else -> emptyList()
    }
    val evaluation = if (bindings.isEmpty()) {
        productionSubjectEvaluation(type, EntryLibraryFilterFeatureContributor)
    } else {
        productionSubjectEvaluation(bindings, EntryLibraryFilterFeatureContributor)
    }
    val feature = DefaultEntryLibraryFilterFeature(evaluation)
    val result = feature.filter(
        EntryLibraryFilterRequest(
            targets = listOf(
                EntryLibraryFilterTarget(type, false, false, false, false, false, false, emptySet()),
            ),
            policy = neutralLibraryFilterPolicy,
        ),
    )

    contractExpectation(result.includedTargetIndices == listOf(0), "Library Filtering must retain a neutral target")
    when (input.subject.integration) {
        ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION -> contractExpectation(
            result.availability.progressSummary.applicableTypes == setOf(type),
            "Library Filtering must expose Progress controls",
        )
        ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION -> contractExpectation(
            result.availability.bookmarking.applicableTypes == setOf(type),
            "Library Filtering must expose Bookmark controls",
        )
        ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION -> contractExpectation(
            result.availability.outsideReleasePeriod.applicableTypes == setOf(type),
            "Library Filtering must expose release-period controls",
        )
        else -> Unit
    }
}

private val neutralLibraryFilterPolicy = EntryLibraryFilterPolicy(
    downloadedOnly = false,
    downloaded = TriState.DISABLED,
    unconsumed = TriState.DISABLED,
    notStarted = TriState.DISABLED,
    bookmarked = TriState.DISABLED,
    completed = TriState.DISABLED,
    outsideReleasePeriod = TriState.DISABLED,
    outsideReleasePeriodEnabled = false,
    tracking = emptyMap(),
)
private val neutralLibraryFilterState = EntryLibraryFilterStateContext(1, false, false, false, false)
private val neutralLibraryTrackingState = EntryLibraryTrackingFilterContext(emptySet(), emptySet())

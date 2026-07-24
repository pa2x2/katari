package mihon.entry.interactions

import io.mockk.coEvery
import io.mockk.mockk
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.validation.FeatureContractExecutionInput
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution

class EntryLibraryProgressContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryLibraryProgressFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        libraryProgressContracts.forEach { contract ->
            sink.add(
                FeatureContractVerifier(
                    FeatureContractReference(ENTRY_LIBRARY_PROGRESS_FEATURE_ID, contract),
                    ::verifyLibraryProgress,
                ),
            )
        }
    }
}

private val libraryProgressContracts: List<FeatureBehaviorContract> = listOf(
    EntryLibraryProgressBehaviorContract,
    EntryLibraryProgressContinueBehaviorContract,
    EntryLibraryProgressBookmarkBehaviorContract,
)

private suspend fun verifyLibraryProgress(input: FeatureContractExecutionInput) = verifyFeatureContract {
    val provider = input.provider(EntryLibraryProgressCapability.definition)
    val bindings = buildList {
        add(EntryLibraryProgressCapability.bind(provider))
        when (input.subject.integration) {
            ENTRY_LIBRARY_PROGRESS_CONTINUE_INTEGRATION -> add(
                EntryContinueCapability.bind(input.provider(EntryContinueCapability.definition)),
            )
            ENTRY_LIBRARY_PROGRESS_BOOKMARK_INTEGRATION -> add(
                EntryBookmarkCapability.bind(input.provider(EntryBookmarkCapability.definition)),
            )
            else -> Unit
        }
    }
    val evaluation = productionSubjectEvaluation(bindings, EntryLibraryProgressFeatureContributor)
    val entry = Entry.create().copy(id = 82L, type = provider.type)
    val chapter = EntryChapter.create().copy(id = 83L, entryId = entry.id, bookmark = true)
    val next = EntryChapter.create().copy(id = 84L, entryId = entry.id)
    val interaction = mockk<EntryLibraryProgressInteraction> {
        coEvery { evidence(entry, listOf(chapter)) } returns EntryLibraryProgressEvidence(true, chapter.id, 0.5f, 10L)
    }
    val continueFeature = mockk<EntryContinueFeature> {
        coEvery { nextTarget(entry) } returns EntryContinueTargetResult.Available(next)
    }
    val feature = DefaultEntryLibraryProgressFeature(evaluation, interaction, continueFeature)
    val result = feature.calculate(entry, listOf(chapter), 0L) as EntryLibraryProgressResolution.Available

    contractExpectation(result.summary.hasStarted, "Library Progress must project provider evidence")
    when (input.subject.integration) {
        ENTRY_LIBRARY_PROGRESS_CONTINUE_INTEGRATION -> contractExpectation(
            result.summary.continueTarget == EntryLibraryContinueTarget.Available(next.id),
            "Library Progress must derive the Continue target",
        )
        ENTRY_LIBRARY_PROGRESS_BOOKMARK_INTEGRATION -> contractExpectation(
            result.summary.bookmarkCount == 1L,
            "Library Progress must derive Bookmark counts",
        )
        else -> Unit
    }
}

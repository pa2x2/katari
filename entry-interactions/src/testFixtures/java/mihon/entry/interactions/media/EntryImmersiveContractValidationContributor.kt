package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryImmersiveContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryImmersiveFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        immersiveContracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_IMMERSIVE_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference, ::verifyImmersive))
            when (item.integration) {
                ENTRY_IMMERSIVE_SOURCE_CONTEXT_INTEGRATION_ID -> sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("entry.immersive.source-context.applicable"),
                        reference,
                        item.integration,
                    ) {
                        listOf(
                            contextEvidence(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT, true),
                            contextEvidence(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT, true),
                            contextEvidence(ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT, true),
                        )
                    },
                )
                ENTRY_IMMERSIVE_ENTRY_CONTEXT_INTEGRATION_ID -> sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("entry.immersive.entry-context.applicable"),
                        reference,
                        item.integration,
                    ) {
                        listOf(
                            contextEvidence(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT, true),
                            contextEvidence(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT, true),
                        )
                    },
                )
                else -> Unit
            }
        }
    }
}

private data class ImmersiveContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
)

private val immersiveContracts = listOf(
    ImmersiveContract(ENTRY_IMMERSIVE_PROVIDER_INTEGRATION_ID, EntryImmersiveProviderBehaviorContract),
    ImmersiveContract(ENTRY_IMMERSIVE_SOURCE_CONTEXT_INTEGRATION_ID, EntryImmersiveSourceBehaviorContract),
    ImmersiveContract(ENTRY_IMMERSIVE_ENTRY_CONTEXT_INTEGRATION_ID, EntryImmersiveBehaviorContract),
    ImmersiveContract(ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID, EntryImmersiveChildBehaviorContract),
    ImmersiveContract(ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID, EntryImmersiveOpenBehaviorContract),
)

private suspend fun verifyImmersive(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val selected = input.provider(EntryImmersiveCapability.definition)
    val integration = input.subject.integration
    val bindings = buildList {
        add(EntryImmersiveCapability.bind(selected))
        if (integration == ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID) {
            add(EntryChildListCapability.bind(input.provider(EntryChildListCapability.definition)))
        }
        if (integration == ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID) {
            add(EntryOpenCapability.bind(input.provider(EntryOpenCapability.definition)))
        }
    }
    val evaluation = productionSubjectEvaluation(bindings, EntryImmersiveFeatureContributor)
    val mode = if (integration == ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID) {
        EntryImmersiveLoadMode.FIRST_READING_CHILD
    } else {
        EntryImmersiveLoadMode.ENTRY
    }
    val processor = RecordingContractImmersiveProcessor(selected.type, mode)
    val interaction = object : EntryImmersiveInteraction {
        override fun processor(type: eu.kanade.tachiyomi.source.entry.EntryType) =
            processor.takeIf { type == processor.type }

        override suspend fun load(context: Context, entry: Entry, chapter: EntryChapter?, source: UnifiedSource) =
            processor.load(context, entry, chapter, source)

        override fun renderer(handle: EntryImmersiveHandle) = processor.renderer(handle)
        override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) = Unit
        override fun release(handle: EntryImmersiveHandle) = Unit
    }
    val child = EntryChapter.create().copy(id = 93L, entryId = 92L)
    val childList = mockk<EntryChildListFeature>(relaxed = true) {
        every { firstReadingChild(any(), any(), any()) } returns EntryFirstChildResult.Available(child)
    }
    val sourceRefresh = mockk<EntrySourceRefreshFeature> {
        coEvery { refresh(any()) } returns EntrySourceRefreshResult.Refreshed(emptyList(), 0, 0, 0, false)
    }
    val feature = DefaultEntryImmersiveFeature(evaluation, interaction, childList, sourceRefresh)
    val entry = Entry.create().copy(id = 92L, type = selected.type)
    val source = mockk<EntryCatalogueSource>(relaxed = true) {
        every { supportsImmersiveFeed } returns true
    }

    contractExpectation(
        feature.sourceAvailability(source) == EntryImmersiveSourceAvailability.Available,
        "Immersive must accept an installed opted-in source",
    )
    contractExpectation(
        feature.availability(EntryImmersiveContext(entry, source)) is EntryImmersiveAvailability.Available,
        "Immersive must expose applicable entry behavior",
    )
    if (integration == ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID) {
        val handle = EntryImmersiveHandle.ImagePages(selected.type, child.id, Unit)
        contractExpectation(
            feature.openTarget(handle) == EntryImmersiveOpenTargetResult.Available(child.id),
            "Immersive must expose an Open target",
        )
        return@verifyFeatureContract
    }
    val loaded = feature.load(
        EntryImmersiveLoadRequest(
            mockk(relaxed = true),
            entry,
            source,
            listOf(child).takeIf { mode == EntryImmersiveLoadMode.FIRST_READING_CHILD }.orEmpty(),
            listOf(entry.id),
        ),
    ) as EntryImmersiveLoadResult.Loaded
    contractExpectation(
        processor.loadedChild == child.takeIf { mode == EntryImmersiveLoadMode.FIRST_READING_CHILD },
        "Immersive must dispatch the graph-selected load shape",
    )
    if (integration == ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID) {
        contractExpectation(
            feature.refreshChildren(entry) == EntryImmersiveChildRefreshResult.Refreshed,
            "Immersive must refresh child-backed entries through Source Refresh",
        )
    }
}

private class RecordingContractImmersiveProcessor(
    override val type: eu.kanade.tachiyomi.source.entry.EntryType,
    override val loadMode: EntryImmersiveLoadMode,
) : EntryImmersiveProcessor {
    override val preloadRadius = 1
    private val contractRenderer = mockk<EntryImmersiveRenderer>()
    var loadedChild: EntryChapter? = null

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle {
        loadedChild = chapter
        return EntryImmersiveHandle.ImagePages(type, chapter?.id, Unit)
    }

    override fun renderer(handle: EntryImmersiveHandle) = contractRenderer
    override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) = Unit
    override fun release(handle: EntryImmersiveHandle) = Unit
}

package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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

class EntryPreviewContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryPreviewFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        previewContracts.forEach { item ->
            val reference = FeatureContractReference(ENTRY_PREVIEW_FEATURE_ID, item.contract)
            sink.add(FeatureContractVerifier(reference, ::verifyPreview))
            if (item.integration == ENTRY_PREVIEW_CONTEXT_INTEGRATION_ID) {
                sink.add(
                    FeatureContractScenario(
                        FeatureContractScenarioId("entry.preview.context.applicable"),
                        reference,
                        item.integration,
                    ) {
                        listOf(
                            contextEvidence(
                                ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT,
                                EntryPreviewSourceRequirement.NONE,
                            ),
                            contextEvidence(ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT, false),
                            contextEvidence(ENTRY_PREVIEW_ENABLED_CONTEXT, true),
                        )
                    },
                )
            }
        }
    }
}

private data class PreviewContract(
    val integration: FeatureIntegrationId,
    val contract: FeatureBehaviorContract,
)

private val previewContracts = listOf(
    PreviewContract(ENTRY_PREVIEW_PROVIDER_INTEGRATION_ID, EntryPreviewProviderBehaviorContract),
    PreviewContract(ENTRY_PREVIEW_CONTEXT_INTEGRATION_ID, EntryPreviewBehaviorContract),
    PreviewContract(ENTRY_PREVIEW_CONFIGURATION_INTEGRATION_ID, EntryPreviewConfigurationBehaviorContract),
    PreviewContract(ENTRY_PREVIEW_CHILD_INTEGRATION_ID, EntryPreviewChildBehaviorContract),
    PreviewContract(ENTRY_PREVIEW_OPEN_INTEGRATION_ID, EntryPreviewOpenBehaviorContract),
)

private suspend fun verifyPreview(
    input: FeatureContractExecutionInput,
): FeatureContractVerificationResult = verifyFeatureContract {
    val selected = input.provider(EntryPreviewCapability.definition)
    val integration = input.subject.integration
    val configuration = if (integration == ENTRY_PREVIEW_CONFIGURATION_INTEGRATION_ID) {
        input.provider(EntryPreviewConfigurationCapability.definition)
    } else {
        null
    }
    val bindings = buildList {
        add(EntryPreviewCapability.bind(selected))
        configuration?.let { add(EntryPreviewConfigurationCapability.bind(it)) }
        if (integration == ENTRY_PREVIEW_CHILD_INTEGRATION_ID) {
            add(EntryChildListCapability.bind(input.provider(EntryChildListCapability.definition)))
        }
        if (integration == ENTRY_PREVIEW_OPEN_INTEGRATION_ID) {
            add(EntryOpenCapability.bind(input.provider(EntryOpenCapability.definition)))
        }
    }
    val evaluation = productionSubjectEvaluation(bindings, EntryPreviewFeatureContributor)
    val mode = if (integration == ENTRY_PREVIEW_CHILD_INTEGRATION_ID) {
        EntryPreviewLoadMode.FIRST_READING_CHILD
    } else {
        EntryPreviewLoadMode.ENTRY
    }
    val processor = RecordingContractPreviewProcessor(
        selected.type,
        mode,
        defaultChapterId = 91L.takeIf { integration == ENTRY_PREVIEW_OPEN_INTEGRATION_ID },
    )
    val interaction = object : EntryPreviewInteraction {
        override fun processor(type: eu.kanade.tachiyomi.source.entry.EntryType) =
            processor.takeIf { type == processor.type }

        override fun configuration(type: eu.kanade.tachiyomi.source.entry.EntryType) =
            configuration?.takeIf { type == configuration.type }

        override suspend fun loadPreview(
            context: Context,
            entry: Entry,
            chapter: EntryChapter?,
            source: UnifiedSource,
            pageCount: Int,
        ) = processor.loadPreview(context, entry, chapter, source, pageCount)

        override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit
        override fun release(handle: EntryPreviewHandle) = Unit
    }
    val child = EntryChapter.create().copy(id = 91L, entryId = 90L)
    val childList = mockk<EntryChildListFeature>(relaxed = true) {
        every { firstReadingChild(any(), any(), any()) } returns EntryFirstChildResult.Available(child)
    }
    val feature = DefaultEntryPreviewFeature(evaluation, interaction, childList)
    val entry = Entry.create().copy(id = 90L, type = selected.type)
    val source = mockk<UnifiedSource>(relaxed = true)

    contractExpectation(feature.isApplicable(selected.type), "Preview must be applicable")
    if (configuration != null) {
        contractExpectation(
            feature.settings.map(EntryPreviewSettings::type) == listOf(selected.type),
            "Preview must expose selected configuration",
        )
    } else {
        val candidates = if (mode == EntryPreviewLoadMode.FIRST_READING_CHILD) {
            listOf(EntryPreviewChildCandidate(entry, child, source))
        } else {
            emptyList()
        }
        val loaded = feature.load(
            EntryPreviewLoadRequest(
                mockk(relaxed = true),
                EntryPreviewContext(entry, source),
                candidates,
                listOf(entry.id),
            ),
        ) as EntryPreviewLoadResult.Loaded
        contractExpectation(
            processor.loadedChild == child.takeIf { mode == EntryPreviewLoadMode.FIRST_READING_CHILD },
            "Preview must dispatch the graph-selected load shape",
        )
        if (integration == ENTRY_PREVIEW_OPEN_INTEGRATION_ID) {
            contractExpectation(
                feature.openTarget(loaded.handle, 0) == EntryPreviewOpenTargetResult.Available(child.id, 0),
                "Preview must expose an Open target",
            )
        }
    }
}

private class RecordingContractPreviewProcessor(
    override val type: eu.kanade.tachiyomi.source.entry.EntryType,
    override val loadMode: EntryPreviewLoadMode,
    private val defaultChapterId: Long?,
) : EntryPreviewProcessor {
    var loadedChild: EntryChapter? = null

    override suspend fun loadPreview(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle {
        loadedChild = chapter
        return EntryPreviewHandle(
            type,
            chapter?.id ?: defaultChapterId,
            listOf(EntryPreviewPage(0, MutableStateFlow(EntryPreviewPageStatus.Ready), MutableStateFlow(1), "preview")),
        )
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit
    override fun release(handle: EntryPreviewHandle) = Unit
}

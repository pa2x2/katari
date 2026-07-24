package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal class DefaultEntryPreviewFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryPreviewInteraction,
    private val childList: EntryChildListFeature,
) : EntryPreviewFeature {
    private val previewTypes = evaluation.applicableProviderTypes<EntryPreviewProcessor>(
        feature = ENTRY_PREVIEW_FEATURE_ID,
        integration = ENTRY_PREVIEW_PROVIDER_INTEGRATION_ID,
        behaviorProjection = EntryPreviewProviderDispatchBehavior.id,
    )
    private val configuredTypes = evaluation.applicableProviderTypes<EntryPreviewConfigurationProvider>(
        feature = ENTRY_PREVIEW_FEATURE_ID,
        integration = ENTRY_PREVIEW_CONFIGURATION_INTEGRATION_ID,
        behaviorProjection = EntryPreviewConfigurationBehavior.id,
    )
    private val childTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_PREVIEW_FEATURE_ID,
        integration = ENTRY_PREVIEW_CHILD_INTEGRATION_ID,
        behaviorProjection = EntryPreviewChildBehavior.id,
    )
    private val openTypes = evaluation.applicableProviderTypes<EntryOpenProcessor>(
        feature = ENTRY_PREVIEW_FEATURE_ID,
        integration = ENTRY_PREVIEW_OPEN_INTEGRATION_ID,
        behaviorProjection = EntryPreviewOpenBehavior.id,
    )

    override val settings: List<EntryPreviewSettings> = configuredTypes
        .sortedBy(EntryType::name)
        .map { type ->
            requireNotNull(interaction.configuration(type)).settings.copy(
                sourceRequirement = requireNotNull(interaction.processor(type)).sourceRequirement,
            )
        }

    init {
        val missingChildList = previewTypes.filter { type ->
            interaction.processor(type)?.loadMode == EntryPreviewLoadMode.FIRST_READING_CHILD && type !in childTypes
        }
        check(missingChildList.isEmpty()) {
            "Child-backed Preview providers require the Preview + Child List relationship; " +
                "missing EntryChildList providers for $missingChildList"
        }
    }

    override fun isApplicable(type: EntryType): Boolean = type in previewTypes

    override fun isOpenApplicable(type: EntryType): Boolean = type in openTypes

    override fun availability(context: EntryPreviewContext): EntryPreviewAvailability {
        val processor = interaction.processor(context.entry.type)
            ?: return EntryPreviewAvailability.Inapplicable(context.entry.type)
        val config = config(context.entry.type)
        return availability(context, processor, config)
    }

    override fun availabilityChanges(context: EntryPreviewContext): Flow<EntryPreviewAvailability> {
        if (!isApplicable(context.entry.type)) return flowOf(EntryPreviewAvailability.Inapplicable(context.entry.type))
        return configChanges(context.entry.type).map { config -> availability(context, config) }
    }

    override suspend fun load(request: EntryPreviewLoadRequest): EntryPreviewLoadResult {
        val entry = request.previewContext.entry
        val processor = interaction.processor(entry.type)
            ?: return EntryPreviewLoadResult.Inapplicable(entry.type)
        val config = config(entry.type)
        contextUnavailableReason(processor, request.previewContext.source, config)?.let { reason ->
            return EntryPreviewLoadResult.ContextuallyUnavailable(reason)
        }
        if (!config.enabled) return EntryPreviewLoadResult.Disabled(config)

        val child = when (processor.loadMode) {
            EntryPreviewLoadMode.ENTRY -> null
            EntryPreviewLoadMode.FIRST_READING_CHILD -> {
                val first = when (
                    val result = childList.firstReadingChild(
                        entry = entry,
                        chapters = request.children.map(EntryPreviewChildCandidate::child),
                        memberIds = request.memberIds,
                    )
                ) {
                    is EntryFirstChildResult.Available -> result.chapter
                    is EntryFirstChildResult.Inapplicable -> error(
                        "Preview graph selected child-backed ${entry.type} without an applicable Child List feature",
                    )
                } ?: return EntryPreviewLoadResult.ContextuallyUnavailable(
                    EntryPreviewUnavailableReason.NoReadingChild,
                )
                request.children.firstOrNull { it.child.id == first.id }
                    ?: error("Child List selected child ${first.id} outside the Preview candidate set")
            }
        }
        val source = child?.source ?: request.previewContext.source
        val contextUnavailable = contextUnavailableReason(processor, source, config)
        if (contextUnavailable != null) {
            return EntryPreviewLoadResult.ContextuallyUnavailable(contextUnavailable)
        }
        return EntryPreviewLoadResult.Loaded(
            interaction.loadPreview(
                context = request.context,
                entry = entry,
                chapter = child?.child,
                source = source,
                pageCount = config.pageCount,
            ),
        )
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) {
        interaction.loadPage(handle, pageIndex)
    }

    override fun openTarget(handle: EntryPreviewHandle, pageIndex: Int): EntryPreviewOpenTargetResult {
        if (handle.entryType !in openTypes) return EntryPreviewOpenTargetResult.Inapplicable(handle.entryType)
        val page = handle.pages.firstOrNull { it.index == pageIndex } ?: return EntryPreviewOpenTargetResult.NotOpenable
        val childId = handle.chapterId ?: return EntryPreviewOpenTargetResult.NotOpenable
        return if (page.canOpen) {
            EntryPreviewOpenTargetResult.Available(childId, page.index)
        } else {
            EntryPreviewOpenTargetResult.NotOpenable
        }
    }

    override fun release(handle: EntryPreviewHandle) {
        interaction.release(handle)
    }

    private fun config(type: EntryType): EntryPreviewConfig =
        if (type in configuredTypes) {
            requireNotNull(interaction.configuration(type)).config()
        } else {
            EntryPreviewConfig.Default
        }

    private fun configChanges(type: EntryType): Flow<EntryPreviewConfig> =
        if (type in configuredTypes) {
            requireNotNull(interaction.configuration(type)).configChanges()
        } else {
            flowOf(EntryPreviewConfig.Default)
        }

    private fun availability(
        context: EntryPreviewContext,
        config: EntryPreviewConfig,
    ): EntryPreviewAvailability {
        val processor = requireNotNull(interaction.processor(context.entry.type))
        return availability(context, processor, config)
    }

    private fun availability(
        context: EntryPreviewContext,
        processor: EntryPreviewProcessor,
        config: EntryPreviewConfig,
    ): EntryPreviewAvailability {
        val unavailable = contextUnavailableReason(processor, context.source, config)
        return when {
            unavailable != null -> EntryPreviewAvailability.ContextuallyUnavailable(config, unavailable)
            !config.enabled -> EntryPreviewAvailability.Disabled(config)
            else -> EntryPreviewAvailability.Available(config)
        }
    }

    private fun contextUnavailableReason(
        processor: EntryPreviewProcessor,
        source: eu.kanade.tachiyomi.source.entry.UnifiedSource,
        config: EntryPreviewConfig,
    ): EntryPreviewUnavailableReason? {
        val sourceSupported = source is EntryPreviewSource
        val applicable = (
            processor.sourceRequirement == EntryPreviewSourceRequirement.NONE || sourceSupported
            ) && config.enabled
        evaluation.requireEntryContextState(
            type = processor.type,
            feature = ENTRY_PREVIEW_FEATURE_ID,
            integration = ENTRY_PREVIEW_CONTEXT_INTEGRATION_ID,
            behaviorProjections = EntryPreviewBehavior.entries.map(EntryPreviewBehavior::id),
            evidence = listOf(
                contextEvidence(ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT, processor.sourceRequirement),
                contextEvidence(ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT, sourceSupported),
                contextEvidence(ENTRY_PREVIEW_ENABLED_CONTEXT, config.enabled),
            ),
            applicable = applicable,
        )
        return if (
            config.enabled &&
            processor.sourceRequirement == EntryPreviewSourceRequirement.PREVIEW_CAPABILITY &&
            !sourceSupported
        ) {
            EntryPreviewUnavailableReason.SourceUnsupported
        } else {
            null
        }
    }
}

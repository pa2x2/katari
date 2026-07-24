package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import kotlinx.coroutines.CancellationException
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
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryImmersiveFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryImmersiveInteraction,
    private val childList: EntryChildListFeature,
    private val sourceRefresh: EntrySourceRefreshFeature,
) : EntryImmersiveFeature {
    private val immersiveTypes = evaluation.applicableProviderTypes<EntryImmersiveProcessor>(
        feature = ENTRY_IMMERSIVE_FEATURE_ID,
        integration = ENTRY_IMMERSIVE_PROVIDER_INTEGRATION_ID,
        behaviorProjection = EntryImmersiveProviderDispatchBehavior.id,
    )
    private val childTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_IMMERSIVE_FEATURE_ID,
        integration = ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID,
        behaviorProjection = EntryImmersiveChildBehavior.id,
    )
    private val childRefreshTypes = evaluation.applicableProviderTypes<EntryChildListProcessor>(
        feature = ENTRY_IMMERSIVE_FEATURE_ID,
        integration = ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID,
        behaviorProjection = EntryImmersiveChildRefreshBehavior.id,
    )
    private val openTypes = evaluation.applicableProviderTypes<EntryOpenProcessor>(
        feature = ENTRY_IMMERSIVE_FEATURE_ID,
        integration = ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID,
        behaviorProjection = EntryImmersiveOpenBehavior.id,
    )

    init {
        val missingChildList = immersiveTypes.filter { type ->
            interaction.processor(type)?.loadMode == EntryImmersiveLoadMode.FIRST_READING_CHILD && type !in childTypes
        }
        check(missingChildList.isEmpty()) {
            "Child-backed Immersive providers require the Immersive + Child List relationship; " +
                "missing EntryChildList providers for $missingChildList"
        }
        check(childTypes == childRefreshTypes) {
            "Immersive child selection and refresh selected different content types"
        }
        immersiveTypes.forEach { type ->
            val radius = requireNotNull(interaction.processor(type)).preloadRadius
            require(radius >= 0) { "Immersive preload radius must be non-negative for $type: $radius" }
        }
    }

    override fun sourceAvailability(source: UnifiedSource?): EntryImmersiveSourceAvailability {
        if (immersiveTypes.isEmpty()) return EntryImmersiveSourceAvailability.NoRuntimeType
        val catalogue = source as? EntryCatalogueSource
        val installed = source != null
        val optedIn = catalogue?.supportsImmersiveFeed == true

        val declaredTypes = source?.supportedEntryTypes()
        val declaredCompatible = declaredTypes == null || declaredTypes.any(immersiveTypes::contains)
        requireSourceContextState(installed, optedIn, declaredTypes)
        return when {
            !installed -> EntryImmersiveSourceAvailability.SourceUnavailable
            !optedIn -> EntryImmersiveSourceAvailability.SourceOptedOut
            !declaredCompatible -> {
                EntryImmersiveSourceAvailability.NoCompatibleDeclaredType(declaredTypes)
            }
            else -> EntryImmersiveSourceAvailability.Available
        }
    }

    override fun availability(context: EntryImmersiveContext): EntryImmersiveAvailability {
        val processor = interaction.processor(context.entry.type)
            ?: return EntryImmersiveAvailability.Inapplicable(context.entry.type)
        requireEntryContextState(context.entry.type, context.source)
        return when (val reason = sourceAvailabilityForEntry(context.source)) {
            null -> EntryImmersiveAvailability.Available(
                preloadRadius = processor.preloadRadius,
                childRequirement = processor.loadMode.toChildRequirement(),
            )
            else -> EntryImmersiveAvailability.ContextuallyUnavailable(reason)
        }
    }

    override fun preloadRadius(type: EntryType): EntryImmersivePreloadRadiusResult {
        val processor = interaction.processor(type)
            ?: return EntryImmersivePreloadRadiusResult.Inapplicable(type)
        return EntryImmersivePreloadRadiusResult.Available(processor.preloadRadius)
    }

    override suspend fun refreshChildren(entry: Entry): EntryImmersiveChildRefreshResult {
        val processor = interaction.processor(entry.type)
            ?: return EntryImmersiveChildRefreshResult.Inapplicable(entry.type)
        if (processor.loadMode != EntryImmersiveLoadMode.FIRST_READING_CHILD || entry.type !in childRefreshTypes) {
            return EntryImmersiveChildRefreshResult.Inapplicable(entry.type)
        }
        return when (
            val result = sourceRefresh.refresh(
                EntrySourceRefreshRequest(
                    entry = entry,
                    fetchDetails = false,
                    fetchChildren = true,
                    manual = false,
                ),
            )
        ) {
            is EntrySourceRefreshResult.Refreshed -> EntryImmersiveChildRefreshResult.Refreshed
            is EntrySourceRefreshResult.SourceUnavailable -> {
                EntryImmersiveChildRefreshResult.ContextuallyUnavailable(
                    EntryImmersiveUnavailableReason.SourceUnavailable,
                )
            }
            is EntrySourceRefreshResult.Failed -> when (val reason = result.reason) {
                EntrySourceRefreshFailure.NoChildren -> EntryImmersiveChildRefreshResult.ContextuallyUnavailable(
                    EntryImmersiveUnavailableReason.NoReadingChild,
                )
                is EntrySourceRefreshFailure.Operation -> EntryImmersiveChildRefreshResult.Failed(reason.error)
            }
        }
    }

    override suspend fun load(request: EntryImmersiveLoadRequest): EntryImmersiveLoadResult {
        val processor = interaction.processor(request.entry.type)
            ?: return EntryImmersiveLoadResult.Inapplicable(request.entry.type)
        requireEntryContextState(request.entry.type, request.source)
        sourceAvailabilityForEntry(request.source)?.let {
            return EntryImmersiveLoadResult.ContextuallyUnavailable(it)
        }
        val source = requireNotNull(request.source)
        val child = when (processor.loadMode) {
            EntryImmersiveLoadMode.ENTRY -> null
            EntryImmersiveLoadMode.FIRST_READING_CHILD -> {
                when (
                    val result = childList.firstReadingChild(
                        entry = request.entry,
                        chapters = request.children,
                        memberIds = request.memberIds,
                    )
                ) {
                    is EntryFirstChildResult.Available -> result.chapter
                    is EntryFirstChildResult.Inapplicable -> error(
                        "Immersive graph selected child-backed ${request.entry.type} without an applicable Child List feature",
                    )
                } ?: return EntryImmersiveLoadResult.ContextuallyUnavailable(
                    EntryImmersiveUnavailableReason.NoReadingChild,
                )
            }
        }
        return try {
            val handle = interaction.load(request.context, request.entry, child, source)
            check(handle.entryType == request.entry.type) {
                "Immersive provider for ${request.entry.type} returned handle for ${handle.entryType}"
            }
            check(handle.chapterId == child?.id) {
                "Immersive provider for ${request.entry.type} returned child ${handle.chapterId}; expected ${child?.id}"
            }
            EntryImmersiveLoadResult.Loaded(handle, child)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            EntryImmersiveLoadResult.Failed(e)
        }
    }

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRendererResult {
        return runCatching { interaction.renderer(handle) }
            .fold(
                onSuccess = EntryImmersiveRendererResult::Available,
                onFailure = EntryImmersiveRendererResult::Failed,
            )
    }

    override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) {
        interaction.persistProgress(handle, progress)
    }

    override fun openTarget(handle: EntryImmersiveHandle): EntryImmersiveOpenTargetResult {
        if (handle.entryType !in openTypes) return EntryImmersiveOpenTargetResult.Inapplicable(handle.entryType)
        val childId = handle.chapterId ?: return EntryImmersiveOpenTargetResult.NotOpenable
        return EntryImmersiveOpenTargetResult.Available(childId)
    }

    override fun release(handle: EntryImmersiveHandle) {
        interaction.release(handle)
    }

    /** Returned Entry.type is authoritative; descriptive source metadata never rejects an actual entry. */
    private fun sourceAvailabilityForEntry(source: UnifiedSource?): EntryImmersiveUnavailableReason? {
        val installed = source != null
        val optedIn = (source as? EntryCatalogueSource)?.supportsImmersiveFeed == true
        return when {
            !installed -> EntryImmersiveUnavailableReason.SourceUnavailable
            !optedIn -> EntryImmersiveUnavailableReason.SourceOptedOut
            else -> null
        }
    }

    private fun requireSourceContextState(
        installed: Boolean,
        optedIn: Boolean,
        declaredTypes: Set<EntryType>?,
    ) {
        immersiveTypes.forEach { type ->
            val declaredCompatible = declaredTypes == null || type in declaredTypes
            evaluation.requireEntryContextState(
                type = type,
                feature = ENTRY_IMMERSIVE_FEATURE_ID,
                integration = ENTRY_IMMERSIVE_SOURCE_CONTEXT_INTEGRATION_ID,
                behaviorProjections = ENTRY_IMMERSIVE_SOURCE_BEHAVIORS.map(EntryImmersiveBehavior::id),
                evidence = listOf(
                    contextEvidence(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT, installed),
                    contextEvidence(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT, optedIn),
                    contextEvidence(ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT, declaredCompatible),
                ),
                applicable = installed && optedIn && declaredCompatible,
            )
        }
    }

    private fun requireEntryContextState(type: EntryType, source: UnifiedSource?) {
        val installed = source != null
        val optedIn = (source as? EntryCatalogueSource)?.supportsImmersiveFeed == true
        evaluation.requireEntryContextState(
            type = type,
            feature = ENTRY_IMMERSIVE_FEATURE_ID,
            integration = ENTRY_IMMERSIVE_ENTRY_CONTEXT_INTEGRATION_ID,
            behaviorProjections = ENTRY_IMMERSIVE_ENTRY_BEHAVIORS.map(EntryImmersiveBehavior::id),
            evidence = listOf(
                contextEvidence(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT, installed),
                contextEvidence(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT, optedIn),
            ),
            applicable = installed && optedIn,
        )
    }
}

private fun EntryImmersiveLoadMode.toChildRequirement(): EntryImmersiveChildRequirement {
    return when (this) {
        EntryImmersiveLoadMode.ENTRY -> EntryImmersiveChildRequirement.NONE
        EntryImmersiveLoadMode.FIRST_READING_CHILD -> EntryImmersiveChildRequirement.FIRST_READING_CHILD
    }
}

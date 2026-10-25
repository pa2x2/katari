package mihon.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryFirstChildResult
import mihon.entry.interactions.runtime.EntryImmersiveLoadMode
import mihon.entry.interactions.source.EntrySourceRefreshFailure
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.source.EntrySourceRefreshRequest
import mihon.entry.interactions.source.EntrySourceRefreshResult
import tachiyomi.domain.entry.model.Entry

internal class DefaultEntryImmersiveFeature(
    private val types: EntryImmersiveTypes,
    private val interaction: EntryImmersiveInteraction,
    private val childList: EntryChildListFeature,
    private val sourceRefresh: EntrySourceRefreshFeature,
) : EntryImmersiveFeature {
    private val immersiveTypes = types.immersive
    private val openTypes = types.open

    init {
        val missingChildList = immersiveTypes.filter { type ->
            interaction.processor(type)?.loadMode == EntryImmersiveLoadMode.FIRST_READING_CHILD &&
                type !in types.childList
        }
        check(missingChildList.isEmpty()) {
            "Child-backed Immersive providers require the Immersive + Child List relationship; " +
                "missing EntryChildList providers for $missingChildList"
        }
        immersiveTypes.forEach { type ->
            val radius = requireNotNull(interaction.processor(type)).preloadRadius
            require(radius >= 0) { "Immersive preload radius must be non-negative for $type: $radius" }
        }
    }

    override fun sourceAvailability(source: UnifiedSource?): EntryImmersiveSourceAvailability {
        if (immersiveTypes.isEmpty()) return EntryImmersiveSourceAvailability.NoRuntimeType
        if (source == null) return EntryImmersiveSourceAvailability.SourceUnavailable

        val declaredTypes = source.supportedEntryTypes()
        return when {
            declaredTypes != null && declaredTypes.none(immersiveTypes::contains) -> {
                EntryImmersiveSourceAvailability.NoCompatibleDeclaredType(declaredTypes)
            }
            else -> EntryImmersiveSourceAvailability.Available
        }
    }

    override fun availability(context: EntryImmersiveContext): EntryImmersiveAvailability {
        val processor = interaction.processor(context.entry.type)
            ?: return EntryImmersiveAvailability.Inapplicable(context.entry.type)
        if (context.source == null) {
            return EntryImmersiveAvailability.ContextuallyUnavailable(EntryImmersiveUnavailableReason.SourceUnavailable)
        }
        return EntryImmersiveAvailability.Available(
            preloadRadius = processor.preloadRadius,
            childRequirement = processor.loadMode.toChildRequirement(),
        )
    }

    override fun preloadRadius(type: EntryType): EntryImmersivePreloadRadiusResult {
        val processor = interaction.processor(type)
            ?: return EntryImmersivePreloadRadiusResult.Inapplicable(type)
        return EntryImmersivePreloadRadiusResult.Available(processor.preloadRadius)
    }

    override suspend fun refreshChildren(entry: Entry): EntryImmersiveChildRefreshResult {
        val processor = interaction.processor(entry.type)
            ?: return EntryImmersiveChildRefreshResult.Inapplicable(entry.type)
        if (processor.loadMode != EntryImmersiveLoadMode.FIRST_READING_CHILD || entry.type !in types.childList) {
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
        val source = request.source
            ?: return EntryImmersiveLoadResult.ContextuallyUnavailable(
                EntryImmersiveUnavailableReason.SourceUnavailable,
            )
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
}

private fun EntryImmersiveLoadMode.toChildRequirement(): EntryImmersiveChildRequirement {
    return when (this) {
        EntryImmersiveLoadMode.ENTRY -> EntryImmersiveChildRequirement.NONE
        EntryImmersiveLoadMode.FIRST_READING_CHILD -> EntryImmersiveChildRequirement.FIRST_READING_CHILD
    }
}

/** The types each Immersive capability applies to, read from the providers the type modules contribute. */
internal class EntryImmersiveTypes(
    val immersive: Set<EntryType>,
    val childList: Set<EntryType>,
    val open: Set<EntryType>,
)

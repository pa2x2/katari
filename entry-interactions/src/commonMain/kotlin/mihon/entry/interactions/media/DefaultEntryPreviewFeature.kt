package mihon.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.child.EntryFirstChildResult
import mihon.entry.interactions.runtime.EntryPreviewLoadMode
import mihon.entry.interactions.runtime.EntryPreviewProcessor

internal class DefaultEntryPreviewFeature(
    private val types: EntryPreviewTypes,
    private val interaction: EntryPreviewInteraction,
    private val childList: EntryChildListFeature,
) : EntryPreviewFeature {
    private val previewTypes = types.preview
    private val configuredTypes = types.configured
    private val openTypes = types.open

    override val settings: List<EntryPreviewSettings> = configuredTypes
        .sortedBy(EntryType::name)
        .map { type ->
            requireNotNull(interaction.configuration(type)).settings.copy(
                sourceRequirement = requireNotNull(interaction.processor(type)).sourceRequirement,
            )
        }

    init {
        val missingChildList = previewTypes.filter { type ->
            interaction.processor(type)?.loadMode == EntryPreviewLoadMode.FIRST_READING_CHILD &&
                type !in types.childList
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
        source: UnifiedSource,
        config: EntryPreviewConfig,
    ): EntryPreviewUnavailableReason? {
        val sourceSupported = source is EntryPreviewSource
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

/** The types each Preview capability applies to, read from the providers the type modules contribute. */
internal class EntryPreviewTypes(
    val preview: Set<EntryType>,
    val configured: Set<EntryType>,
    val childList: Set<EntryType>,
    val open: Set<EntryType>,
)

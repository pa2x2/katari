package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class ProviderBackedEntryPreviewInteraction(
    private val processors: Map<EntryType, EntryPreviewProcessor>,
    private val configurations: Map<EntryType, EntryPreviewConfigurationProvider>,
) : EntryPreviewInteraction {
    override fun processor(type: EntryType): EntryPreviewProcessor? = processors[type]

    override fun configuration(type: EntryType): EntryPreviewConfigurationProvider? = configurations[type]

    override suspend fun loadPreview(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle {
        val processor = processors.requireProcessor("preview", entry.type)
        return processor.loadPreview(context, entry, chapter, source, pageCount)
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) {
        val processor = processors.requireProcessor("preview", handle.entryType)
        processor.loadPage(handle, pageIndex)
    }

    override fun release(handle: EntryPreviewHandle) {
        processors.requireProcessor("preview", handle.entryType).release(handle)
    }
}

internal class ProviderBackedEntryImmersiveInteraction(
    private val processors: Map<EntryType, EntryImmersiveProcessor>,
) : EntryImmersiveInteraction {
    override fun processor(type: EntryType): EntryImmersiveProcessor? = processors[type]

    override suspend fun load(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle {
        val processor = processors.requireProcessor("immersive feed", entry.type)
        processor.requireMatchingEntryType("immersive feed", entry, processors.keys)
        return processor.load(context, entry, chapter, source)
    }

    override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
        return processors.requireProcessor("immersive feed", handle.entryType).renderer(handle)
    }

    override suspend fun persistProgress(
        handle: EntryImmersiveHandle,
        progress: EntryImmersiveProgress,
    ) {
        processors.requireProcessor("immersive feed", handle.entryType)
            .persistProgress(handle, progress)
    }

    override fun release(handle: EntryImmersiveHandle) {
        processors.requireProcessor("immersive feed", handle.entryType).release(handle)
    }
}

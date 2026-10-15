package mihon.entry.interactions.download

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryDownloadOptionsFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryDownloadInteraction,
) : EntryDownloadOptionsFeature {

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun resolve(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
    ): EntryDownloadOptionsResolution {
        if (!isApplicable(entry.type)) return EntryDownloadOptionsResolution.Inapplicable
        return interaction.resolveDownloadOptions(context, entry, chapter)
            ?.let(EntryDownloadOptionsResolution::Resolved)
            ?: EntryDownloadOptionsResolution.ContextuallyUnavailable
    }

    override suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ): EntryDownloadOptionsActionResult {
        if (!isApplicable(entry.type)) return EntryDownloadOptionsActionResult.Inapplicable
        interaction.downloadWithOptions(entry, chapters, selection, startNow)
        return EntryDownloadOptionsActionResult.Performed
    }
}

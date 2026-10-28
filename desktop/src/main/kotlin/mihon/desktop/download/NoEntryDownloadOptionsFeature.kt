package mihon.desktop.download

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.download.EntryDownloadOptionSelection
import mihon.entry.interactions.download.EntryDownloadOptionsActionResult
import mihon.entry.interactions.download.EntryDownloadOptionsFeature
import mihon.entry.interactions.download.EntryDownloadOptionsResolution
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Download options as a feature that applies to no entry type, as the shared feature answers for a type without them,
 * so the entry screen doesn't ask which version of a chapter to download. Stands in with the other download features.
 */
@Inject
class NoEntryDownloadOptionsFeature : EntryDownloadOptionsFeature {
    override fun isApplicable(type: EntryType): Boolean = false

    override suspend fun resolve(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        selection: EntryDownloadOptionSelection?,
    ): EntryDownloadOptionsResolution = EntryDownloadOptionsResolution.Inapplicable

    override suspend fun download(
        entry: Entry,
        chapters: List<EntryChapter>,
        selection: EntryDownloadOptionSelection,
        startNow: Boolean,
    ): EntryDownloadOptionsActionResult = EntryDownloadOptionsActionResult.Inapplicable
}

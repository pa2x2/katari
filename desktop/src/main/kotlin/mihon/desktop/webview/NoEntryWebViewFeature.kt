package mihon.desktop.webview

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.SiteStorageSource
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.EntryWebViewHeadersResolution
import mihon.entry.interactions.source.EntryWebViewResolution
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Desktop has no web view, so no source's site opens in one: every source answers as one without a site to show, and
 * screens don't offer it.
 */
@Inject
class NoEntryWebViewFeature : EntryWebViewFeature {
    override fun resolveEntry(entry: Entry): EntryWebViewResolution = EntryWebViewResolution.Unsupported(entry.source)

    override fun resolveChild(owner: Entry, child: EntryChapter): EntryChildWebViewResolution =
        EntryChildWebViewResolution.Unsupported(owner.source)

    override fun resolveHeaders(sourceId: Long): EntryWebViewHeadersResolution =
        EntryWebViewHeadersResolution.Unsupported

    override fun siteStorageSource(sourceId: Long): SiteStorageSource? = null
}

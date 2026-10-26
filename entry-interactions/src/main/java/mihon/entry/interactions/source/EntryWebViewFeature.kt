package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.WebViewSource
import eu.kanade.tachiyomi.util.system.toWebViewHeaders
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal data class EntryWebViewContext(val installed: Boolean, val supported: Boolean)

internal data class EntryChildWebViewContext(val installed: Boolean, val supported: Boolean)

internal class DefaultEntryWebViewFeature(
    private val sourceManager: SourceManager,
) : EntryWebViewFeature {

    override fun resolveEntry(entry: Entry): EntryWebViewResolution {
        val source = sourceManager.get(entry.source)
        val provider = source as? WebViewSource
        if (source == null) return EntryWebViewResolution.Missing(entry.source)
        if (provider == null) return EntryWebViewResolution.Unsupported(entry.source)

        return runCatching {
            EntryWebViewResolution.Available(
                sourceId = source.id,
                url = provider.getContentUrl(entry.toSEntry()),
                headers = provider.getWebViewHeaders().toWebViewHeaders(),
            )
        }.getOrElse { EntryWebViewResolution.Failed(entry.source, it) }
    }

    override fun resolveChild(owner: Entry, child: EntryChapter): EntryChildWebViewResolution {
        val source = sourceManager.get(owner.source)
        val provider = source as? ChapterWebViewSource
        if (source == null) return EntryChildWebViewResolution.Missing(owner.source)
        if (provider == null) return EntryChildWebViewResolution.Unsupported(owner.source)

        return runCatching {
            EntryChildWebViewResolution.Available(
                sourceId = owner.source,
                url = provider.getChapterUrl(child.toSEntryChapter()),
            )
        }.getOrElse { EntryChildWebViewResolution.Failed(owner.source, it) }
    }

    override fun resolveHeaders(sourceId: Long): EntryWebViewHeadersResolution {
        val source = sourceManager.get(sourceId)
        val provider = source as? WebViewSource
        if (source == null) return EntryWebViewHeadersResolution.Missing
        if (provider == null) return EntryWebViewHeadersResolution.Unsupported
        return runCatching { EntryWebViewHeadersResolution.Available(provider.getWebViewHeaders().toWebViewHeaders()) }
            .getOrElse(EntryWebViewHeadersResolution::Failed)
    }
}

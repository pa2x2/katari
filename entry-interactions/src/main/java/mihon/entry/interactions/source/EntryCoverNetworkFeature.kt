package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.EntryImageSource
import tachiyomi.domain.source.service.SourceManager

internal data class EntryCoverNetworkContext(val installed: Boolean, val supported: Boolean)

internal class DefaultEntryCoverNetworkFeature(
    private val sourceManager: SourceManager,
) : EntryCoverNetworkFeature {

    override fun resolve(sourceId: Long): EntryCoverNetworkResolution {
        val source = sourceManager.get(sourceId)
        val provider = source as? EntryImageSource
        if (source == null) return EntryCoverNetworkResolution.Missing(sourceId)
        if (provider == null) return EntryCoverNetworkResolution.Unsupported(sourceId)

        return runCatching {
            EntryCoverNetworkResolution.Available(
                sourceId = sourceId,
                callFactory = provider.client,
                headers = provider.headers,
            )
        }.getOrElse { EntryCoverNetworkResolution.Failed(sourceId, it) }
    }
}

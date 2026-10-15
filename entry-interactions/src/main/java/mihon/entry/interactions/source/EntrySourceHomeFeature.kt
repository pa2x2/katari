package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.SourceHomePage
import tachiyomi.domain.source.service.SourceManager

internal enum class SourceHomeUrlState {
    AVAILABLE,
    ABSENT,
    FAILED,
}

internal data class SourceHomeContext(
    val installed: Boolean,
    val supported: Boolean,
    val urlState: SourceHomeUrlState,
)

internal class DefaultEntrySourceHomeFeature(
    private val sourceManager: SourceManager,
) : EntrySourceHomeFeature {

    override fun resolve(sourceId: Long): EntrySourceHomeResolution {
        val source = sourceManager.get(sourceId)
            ?: return result(sourceId, installed = false, provider = null, url = null)
        val provider = source as? SourceHomePage
            ?: return result(sourceId, installed = true, provider = null, url = null)
        val url = try {
            provider.getHomeUrl()
        } catch (error: Throwable) {
            return EntrySourceHomeResolution.Failed(sourceId, error)
        }
        return result(sourceId, installed = true, provider = provider, url = url)
    }

    private fun result(
        sourceId: Long,
        installed: Boolean,
        provider: SourceHomePage?,
        url: String?,
    ): EntrySourceHomeResolution {
        val hasUrl = !url.isNullOrBlank()
        return when {
            !installed -> EntrySourceHomeResolution.Missing(sourceId)
            provider == null -> EntrySourceHomeResolution.Unsupported(sourceId)
            !hasUrl -> EntrySourceHomeResolution.NoUrl(sourceId)
            else -> EntrySourceHomeResolution.Available(sourceId, provider.name, url)
        }
    }
}

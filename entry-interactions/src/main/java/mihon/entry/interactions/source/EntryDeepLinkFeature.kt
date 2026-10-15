package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.EntryUriType
import eu.kanade.tachiyomi.source.entry.ResolvableSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.service.awaitInitialized

internal enum class EntryDeepLinkMatchState {
    MATCHED,
    NOT_MATCHED,
    FAILED,
}

internal data class EntryDeepLinkContext(
    val hasResolver: Boolean,
    val matchState: EntryDeepLinkMatchState,
)

internal class DefaultEntryDeepLinkFeature(
    private val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
    private val entryChapterRepository: EntryChapterRepository,
    private val sourceRefresh: EntrySourceRefreshFeature,
) : EntryDeepLinkFeature {

    override suspend fun resolve(uri: String): EntryDeepLinkResolution {
        // A link that starts the app would otherwise find no source to open it
        sourceManager.awaitInitialized()
        val resolvers = sourceManager.getAll().filterIsInstance<ResolvableSource>()
        if (resolvers.isEmpty()) {
            return EntryDeepLinkResolution.NoMatch
        }

        val match = try {
            resolvers.firstNotNullOfOrNull { source ->
                source.getUriType(uri).takeUnless { it == EntryUriType.Unknown }?.let { source to it }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            return EntryDeepLinkResolution.Failed(error)
        }
        match ?: return EntryDeepLinkResolution.NoMatch

        return try {
            resolveMatched(uri, match.first, match.second)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            EntryDeepLinkResolution.Failed(error)
        }
    }

    private suspend fun resolveMatched(
        uri: String,
        source: ResolvableSource,
        uriType: EntryUriType,
    ): EntryDeepLinkResolution {
        val entry = source.getEntry(uri)?.let { networkToLocalEntry(it.toEntry(source.id)) }
            ?: return EntryDeepLinkResolution.NoMatch
        val child = if (uriType == EntryUriType.Chapter) {
            source.getChapter(uri)?.let { getChapter(it, entry) }
        } else {
            null
        }
        return EntryDeepLinkResolution.Resolved(entry, child?.id)
    }

    private suspend fun getChapter(sourceChild: SEntryChapter, entry: Entry): EntryChapter? {
        return entryChapterRepository.getChapterByUrlAndEntryId(sourceChild.url, entry.id)
            ?: when (
                val refresh = sourceRefresh.refresh(
                    EntrySourceRefreshRequest(
                        entry,
                        manual = false,
                    ),
                )
            ) {
                is EntrySourceRefreshResult.Refreshed -> {
                    refresh.insertedChildren.find { it.url == sourceChild.url }
                }
                is EntrySourceRefreshResult.SourceUnavailable -> throw SourceNotInstalledException()
                is EntrySourceRefreshResult.Failed -> when (val reason = refresh.reason) {
                    EntrySourceRefreshFailure.NoChildren -> throw NoChaptersException()
                    is EntrySourceRefreshFailure.Operation -> throw reason.error
                }
            }
    }
}

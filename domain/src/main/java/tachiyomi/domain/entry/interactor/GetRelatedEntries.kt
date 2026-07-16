package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.adapter.toEntry
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.identity
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.domain.source.service.SourceManager

class GetRelatedEntries(
    private val sourceManager: SourceManager,
    private val networkToLocalEntry: NetworkToLocalEntry,
) {

    suspend fun await(entry: Entry): List<Entry> = withIOContext {
        val source = sourceManager.get(entry.source) ?: throw SourceNotInstalledException()
        val relatedEntriesSource = source as? RelatedEntriesSource
            ?: throw RelatedEntriesNotSupportedException()

        relatedEntriesSource.getRelatedEntries(entry.toSEntry())
            .map { it.toEntry(source.id) }
            .distinctBy(Entry::identity)
            .let { networkToLocalEntry(it) }
    }
}

class RelatedEntriesNotSupportedException : Exception()

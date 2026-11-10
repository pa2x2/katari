package app.katari.entry.interactions.runtime.usage

import app.katari.core.metro.AppCoroutineScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.domain.source.identity.SourceIdentityRepository
import tachiyomi.domain.source.identity.SourceProviderKind

/** Starts the [ReadingSessionReport] of each reader or player session. */
@Inject
@SingleIn(AppScope::class)
class ReadingSessionReports(
    private val sourceIdentities: SourceIdentityRepository,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start(entryType: EntryType): ReadingSessionReport = ReadingSessionReport(entryType, ::sourceKind, scope)

    // The local source is built into the app and no extension records itself as its provider
    private suspend fun sourceKind(sourceId: Long): String? =
        if (sourceId == LOCAL_SOURCE_ID) {
            SourceProviderKind.BUILT_IN.key
        } else {
            sourceIdentities.find(sourceId)?.provider?.kind?.key
        }
}

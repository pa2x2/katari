package eu.kanade.tachiyomi.data.notification

import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.state.EntryConsumptionFeature
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository

/** Graph entry points of the notification action receiver, which Android constructs, and of its pending intents. */
interface NotificationReceiverGraph {
    @AppCoroutineScope
    val appScope: CoroutineScope

    val entryMergeNavigationFeature: EntryMergeNavigationFeature
    val entryRepository: EntryRepository
    val entryChapterRepository: EntryChapterRepository
    val entryConsumptionFeature: EntryConsumptionFeature
    val entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature
    val entryDownloadActionFeature: EntryDownloadActionFeature
    val entryOpenFeature: EntryOpenFeature
}

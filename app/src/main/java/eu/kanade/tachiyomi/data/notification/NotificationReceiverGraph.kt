package eu.kanade.tachiyomi.data.notification

import app.katari.core.metro.AppCoroutineScope
import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.entry.interactions.merge.EntryMergeNavigationFeature
import app.katari.entry.interactions.navigation.EntryOpenFeature
import app.katari.entry.interactions.state.EntryConsumptionFeature
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.MetadataUpdateRunner

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
    val backupRestoreRunner: BackupRestoreRunner
    val libraryUpdateQueue: LibraryUpdateQueue
    val metadataUpdateRunner: MetadataUpdateRunner
}

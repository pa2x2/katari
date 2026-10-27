package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.domain.track.service.AndroidDelayedTrackingUpdateScheduler
import eu.kanade.tachiyomi.data.backup.create.AndroidBackupCreateSchedule
import eu.kanade.tachiyomi.data.backup.restore.AndroidBackupRestoreRunner
import eu.kanade.tachiyomi.data.chapterlist.ChapterListWalkWorkScheduler
import eu.kanade.tachiyomi.data.library.AndroidLibraryUpdateQueue
import eu.kanade.tachiyomi.data.library.AndroidLibraryUpdateSchedule
import eu.kanade.tachiyomi.data.library.AndroidMetadataUpdateRunner
import eu.kanade.tachiyomi.data.statistics.AndroidStatisticsRecapSchedule
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.service.MetadataUpdateRunner
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule
import tachiyomi.domain.track.service.DelayedTrackingUpdateScheduler

/** The WorkManager implementations of the background work that screens and shared code start, stop and observe. */
@BindingContainer
interface WorkBindings {
    @Binds
    val AndroidLibraryUpdateQueue.libraryUpdateQueue: LibraryUpdateQueue

    @Binds
    val AndroidLibraryUpdateSchedule.libraryUpdateSchedule: LibraryUpdateSchedule

    @Binds
    val AndroidMetadataUpdateRunner.metadataUpdateRunner: MetadataUpdateRunner

    @Binds
    val AndroidBackupCreateSchedule.backupCreateSchedule: BackupCreateSchedule

    @Binds
    val AndroidBackupRestoreRunner.backupRestoreRunner: BackupRestoreRunner

    @Binds
    val AndroidStatisticsRecapSchedule.statisticsRecapSchedule: StatisticsRecapSchedule

    @Binds
    val AndroidDelayedTrackingUpdateScheduler.delayedTrackingUpdateScheduler: DelayedTrackingUpdateScheduler

    @Binds
    val ChapterListWalkWorkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler
}

package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.data.backup.create.AndroidBackupCreateSchedule
import eu.kanade.tachiyomi.data.backup.restore.AndroidBackupRestoreRunner
import eu.kanade.tachiyomi.data.chapterlist.ChapterListWalkWorkScheduler
import eu.kanade.tachiyomi.data.library.AndroidLibraryUpdateSchedule
import eu.kanade.tachiyomi.data.library.AndroidLibraryUpdateWorkChain
import eu.kanade.tachiyomi.data.library.AndroidMetadataUpdateRunner
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifications
import eu.kanade.tachiyomi.data.library.notification.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.library.queue.LibraryUpdateWorkChain
import eu.kanade.tachiyomi.data.statistics.AndroidStatisticsRecapSchedule
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.entry.service.ChapterListWalkScheduler
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.service.MetadataUpdateRunner
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

/** The WorkManager implementations of the background work that screens and shared code start, stop and observe. */
@BindingContainer
interface WorkBindings {
    @Binds
    val AndroidLibraryUpdateWorkChain.libraryUpdateWorkChain: LibraryUpdateWorkChain

    // Unscoped: each update gets its own, which keeps that update's progress notification.
    @Binds
    val LibraryUpdateNotifier.libraryUpdateNotifications: LibraryUpdateNotifications

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
    val ChapterListWalkWorkScheduler.chapterListWalkScheduler: ChapterListWalkScheduler
}

package eu.kanade.tachiyomi.data.schedule

import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

/** Graph entry points of the clock change receiver, which Android constructs. */
interface ClockChangeScheduleGraph {
    val backupCreateSchedule: BackupCreateSchedule
    val libraryUpdateSchedule: LibraryUpdateSchedule
    val statisticsRecapSchedule: StatisticsRecapSchedule
}

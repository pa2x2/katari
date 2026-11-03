package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import mihon.desktop.statistics.InProcessStatisticsRecapSchedule
import mihon.desktop.work.ResumableWork
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

@BindingContainer
interface StatisticsBindings {
    @Binds
    val InProcessStatisticsRecapSchedule.statisticsRecapSchedule: StatisticsRecapSchedule

    @Binds
    @IntoSet
    val InProcessStatisticsRecapSchedule.resumableStatisticsRecaps: ResumableWork
}

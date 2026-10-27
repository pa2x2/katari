package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.statistics.NoStatisticsRecapSchedule
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

@BindingContainer
interface StatisticsBindings {
    @Binds
    val NoStatisticsRecapSchedule.statisticsRecapSchedule: StatisticsRecapSchedule
}

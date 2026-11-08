package app.katari.desktop.di

import app.katari.desktop.statistics.InProcessStatisticsRecapSchedule
import app.katari.desktop.work.ResumableWork
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

@BindingContainer
interface StatisticsBindings {
    @Binds
    val InProcessStatisticsRecapSchedule.statisticsRecapSchedule: StatisticsRecapSchedule

    @Binds
    @IntoSet
    val InProcessStatisticsRecapSchedule.resumableStatisticsRecaps: ResumableWork
}

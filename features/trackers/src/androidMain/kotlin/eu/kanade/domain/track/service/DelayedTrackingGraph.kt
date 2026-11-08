package eu.kanade.domain.track.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

/** Graph entry point of the delayed tracking job, which WorkManager constructs. */
@ContributesTo(AppScope::class)
interface DelayedTrackingGraph {
    fun inject(job: DelayedTrackingUpdateJob)
}

package eu.kanade.domain.track.service

/** Graph entry point of the delayed tracking job, which WorkManager constructs. */
interface DelayedTrackingGraph {
    fun inject(job: DelayedTrackingUpdateJob)
}

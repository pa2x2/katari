package tachiyomi.domain.track.service

/** Sends the tracking progress that couldn't reach its tracker once the device is online again. */
fun interface DelayedTrackingUpdateScheduler {
    fun setupTask()
}

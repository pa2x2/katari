package tachiyomi.domain.track.service

import mihon.core.common.platform.PlatformFacility

/** Sends the tracking progress that couldn't reach its tracker once the device is online again. */
@PlatformFacility
fun interface DelayedTrackingUpdateScheduler {
    fun setupTask()
}

package tachiyomi.domain.library.service

import app.katari.core.common.platform.PlatformFacility

/** Refreshes the details of every entry in the active profile's library, without their chapter lists. */
@PlatformFacility
interface MetadataUpdateRunner {
    /** False when a refresh already runs, so none was started. */
    suspend fun startNow(): Boolean

    suspend fun stop()
}

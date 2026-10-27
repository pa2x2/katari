package tachiyomi.domain.library.service

/** Refreshes the details of every entry in the active profile's library, without their chapter lists. */
interface MetadataUpdateRunner {
    /** False when a refresh already runs, so none was started. */
    suspend fun startNow(): Boolean

    suspend fun stop()
}

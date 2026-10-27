package eu.kanade.tachiyomi.data.track

/**
 * Where a tracker tells the user that work they didn't wait for failed, such as sending a read chapter to the service.
 * Called from any thread.
 */
interface TrackerFailureMessages {
    suspend fun show(message: String?)
}

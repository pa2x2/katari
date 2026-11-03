package eu.kanade.tachiyomi.data.track

import dev.zacsweers.metro.Inject
import mihon.core.platform.message.OutsideScreenMessages

/**
 * Where a tracker tells the user that work they didn't wait for failed, such as sending a read chapter to the service.
 * Called from any thread.
 */
@Inject
class TrackerFailureMessages(private val messages: OutsideScreenMessages) {
    // A failure without a message still shows, empty, as it always has on Android.
    suspend fun show(message: String?) {
        messages.show(message.orEmpty())
    }
}

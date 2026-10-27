package mihon.desktop.tracking

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.track.TrackerFailureMessages
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Desktop has no app-wide place yet for a message that no screen asked for, so a failure a tracker reports in the
 * background is only logged. Stands in until desktop can show such messages.
 */
@Inject
class NoTrackerFailureMessages : TrackerFailureMessages {
    override suspend fun show(message: String?) {
        logcat(LogPriority.WARN) { "Tracker failure: $message" }
    }
}

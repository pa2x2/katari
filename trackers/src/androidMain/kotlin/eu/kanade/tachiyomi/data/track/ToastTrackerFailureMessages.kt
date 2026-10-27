package eu.kanade.tachiyomi.data.track

import android.app.Application
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.util.lang.withUIContext

@Inject
class ToastTrackerFailureMessages(private val application: Application) : TrackerFailureMessages {
    override suspend fun show(message: String?) {
        withUIContext { application.toast(message) }
    }
}

package mihon.app.message

import android.app.Application
import android.widget.Toast
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.toast
import mihon.core.platform.message.OutsideScreenMessages
import tachiyomi.core.common.util.lang.withUIContext

@Inject
class ToastOutsideScreenMessages(private val application: Application) : OutsideScreenMessages {
    override suspend fun show(text: String, long: Boolean) {
        withUIContext { application.toast(text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT) }
    }
}

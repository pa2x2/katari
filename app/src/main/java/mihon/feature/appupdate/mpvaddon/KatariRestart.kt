package mihon.feature.appupdate.mpvaddon

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

/**
 * Starts Katari afresh in a new process, which is the only way to swap mpv's libraries once they are loaded. The
 * relaunch is queued before this process exits, so Android starts the new one for it.
 */
internal fun restartKatari(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    val component = launch.component ?: return
    context.startActivity(Intent.makeRestartActivityTask(component))
    exitProcess(0)
}

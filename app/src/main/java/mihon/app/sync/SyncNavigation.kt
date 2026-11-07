package mihon.app.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.ui.main.MainActivity

/** The places sync's notifications open in the main activity, in whichever profile it shows. */
object SyncNavigation {
    const val ACTION_OPEN_SYNC = "app.katari.action.OPEN_SYNC"
    const val ACTION_OPEN_SYNC_QUESTIONS = "app.katari.action.OPEN_SYNC_QUESTIONS"

    /** Opens what [action] names, dismissing notification [notificationId]. */
    fun pendingIntent(context: Context, action: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("notificationId", notificationId)
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

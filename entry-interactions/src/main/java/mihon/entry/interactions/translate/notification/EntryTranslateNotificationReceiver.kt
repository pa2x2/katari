package mihon.entry.interactions.translate.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslateRuntimeAvailability
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Pauses, resumes or cancels the translation queue from the buttons of its notifications. */
class EntryTranslateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = Action.entries.firstOrNull { it.intentAction == intent.action } ?: return
        val result = goAsync()
        scope.launch {
            try {
                EntryTranslateRuntimeAvailability.awaitInstalled()
                val feature = Injekt.get<EntryTranslateFeature>()
                when (action) {
                    Action.Pause -> feature.pause()
                    Action.Resume -> feature.resume()
                    Action.CancelAll -> feature.cancelAll()
                }
            } finally {
                result.finish()
            }
        }
    }

    internal enum class Action(val intentAction: String) {
        Pause("mihon.entry.interactions.translate.PAUSE"),
        Resume("mihon.entry.interactions.translate.RESUME"),
        CancelAll("mihon.entry.interactions.translate.CANCEL_ALL"),
        ;

        fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            ordinal,
            Intent(context, EntryTranslateNotificationReceiver::class.java).setAction(intentAction),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

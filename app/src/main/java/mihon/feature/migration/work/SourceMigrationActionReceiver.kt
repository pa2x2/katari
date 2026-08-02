package mihon.feature.migration.work

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mihon.feature.migration.session.model.SourceMigrationSessionId
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceMigrationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
            ?.takeIf(String::isNotBlank)
            ?.let(::SourceMigrationSessionId)
            ?: return
        if (intent.action != ACTION_PAUSE_DISCOVERY) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Injekt.get<SourceMigrationWorkScheduler>().pauseDiscovery(sessionId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_PAUSE_DISCOVERY = "app.katari.action.PAUSE_SOURCE_MIGRATION_DISCOVERY"
        private const val EXTRA_SESSION_ID = "session_id"

        fun pauseDiscoveryIntent(context: Context, sessionId: SourceMigrationSessionId): PendingIntent {
            val intent = Intent(context, SourceMigrationActionReceiver::class.java)
                .setAction(ACTION_PAUSE_DISCOVERY)
                .putExtra(EXTRA_SESSION_ID, sessionId.value)
            return PendingIntent.getBroadcast(
                context,
                sessionId.value.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

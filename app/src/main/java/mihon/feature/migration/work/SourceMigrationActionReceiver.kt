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
        if (intent.action != ACTION_PAUSE_DISCOVERY && intent.action != ACTION_PAUSE_EXECUTION) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = Injekt.get<SourceMigrationWorkScheduler>()
                when (intent.action) {
                    ACTION_PAUSE_DISCOVERY -> scheduler.pauseDiscovery(sessionId)
                    ACTION_PAUSE_EXECUTION -> scheduler.pauseExecution(sessionId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_PAUSE_DISCOVERY = "app.katari.action.PAUSE_SOURCE_MIGRATION_DISCOVERY"
        private const val ACTION_PAUSE_EXECUTION = "app.katari.action.PAUSE_SOURCE_MIGRATION_EXECUTION"
        private const val EXTRA_SESSION_ID = "session_id"

        fun pauseDiscoveryIntent(context: Context, sessionId: SourceMigrationSessionId): PendingIntent {
            return pauseIntent(context, sessionId, ACTION_PAUSE_DISCOVERY, DISCOVERY_REQUEST_OFFSET)
        }

        fun pauseExecutionIntent(context: Context, sessionId: SourceMigrationSessionId): PendingIntent {
            return pauseIntent(context, sessionId, ACTION_PAUSE_EXECUTION, EXECUTION_REQUEST_OFFSET)
        }

        private fun pauseIntent(
            context: Context,
            sessionId: SourceMigrationSessionId,
            action: String,
            requestOffset: Int,
        ): PendingIntent {
            val intent = Intent(context, SourceMigrationActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_SESSION_ID, sessionId.value)
            return PendingIntent.getBroadcast(
                context,
                sessionId.value.hashCode() + requestOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private const val DISCOVERY_REQUEST_OFFSET = 1
        private const val EXECUTION_REQUEST_OFFSET = 2
    }
}

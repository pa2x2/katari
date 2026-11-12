package app.katari.android.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts the app's process at boot, whose start resumes sync, and sync starts [InstantSyncService] while instant sync
 * is on. Nothing else to do here: while this broadcast is handled, Android lets the app start the service from the
 * background.
 */
class SyncBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

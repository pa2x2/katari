package app.katari.android.sync

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import app.katari.android.di.appGraph
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notify
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tachiyomi.i18n.*

/**
 * Keeps the app running while instant sync is on, so the live channel stays open with the app closed and other
 * devices' changes and messages come at once ([app.katari.sync.client.trigger.SyncTriggers.connectedInBackground]).
 * Android lets an app run so only under a notification ([InstantSyncNotification]). The service type is
 * `remoteMessaging`: `dataSync` runs at most six hours a day and can't start at boot.
 *
 * Android starts it again after stopping it for memory, and at boot the app's start does ([SyncBootReceiver]).
 */
class InstantSyncService : Service() {
    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        val graph = appGraph
        val notification = graph.instantSyncNotification
        ServiceCompat.startForeground(
            this,
            Notifications.ID_SYNC_CONNECTION,
            notification.build(PlatformStrings.sync_instant_connecting),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            } else {
                0
            },
        )
        graph.syncTriggers.connectedInBackground(true)
        scope.launch {
            notification.states().collect { notify(Notifications.ID_SYNC_CONNECTION, notification.build(it)) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        appGraph.syncTriggers.connectedInBackground(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

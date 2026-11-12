package app.katari.android.sync

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.notification.AppNotificationKind
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.trigger.LiveUpdates
import app.katari.sync.client.trigger.LiveUpdatesState
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * The notification [InstantSyncService] runs under: how the connection stands, and a button that opens the settings
 * of its channel, where the person turns it off and the service runs on without it.
 */
@Inject
class InstantSyncNotification(
    private val application: Application,
    private val live: LiveUpdates,
    private val network: NetworkConnectivity,
    private val preferences: SyncPreferences,
) {
    fun states(): Flow<StringResource> = combine(
        live.states,
        network.state(),
        preferences.changesOnMobileData.changes(),
    ) { live, network, onMobileData ->
        when {
            !network.hasNetwork -> PlatformStrings.sync_instant_waiting_for_network
            !onMobileData && !network.countsAsWifi -> PlatformStrings.sync_instant_waiting_for_wifi
            live == LiveUpdatesState.OPEN -> PlatformStrings.sync_instant_connected
            else -> PlatformStrings.sync_instant_connecting
        }
    }
        .distinctUntilChanged()

    fun build(state: StringResource): Notification =
        application.notificationBuilder(AppNotificationKind.SYNC_CONNECTION.id) {
            setSmallIcon(R.drawable.ic_katari)
            setContentTitle(application.stringResource(PlatformStrings.sync_instant_title))
            setContentText(application.stringResource(state))
            setSubText(application.stringResource(PlatformStrings.sync_instant_hide_hint))
            setOngoing(true)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            setContentIntent(
                SyncNavigation.pendingIntent(
                    application,
                    SyncNavigation.ACTION_OPEN_SYNC,
                    Notifications.ID_SYNC_CONNECTION,
                ),
            )
            addAction(0, application.stringResource(PlatformStrings.sync_instant_hide), channelSettings())
        }.build()

    private fun channelSettings(): PendingIntent = PendingIntent.getActivity(
        application,
        Notifications.ID_SYNC_CONNECTION,
        channelSettingsIntent(application),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** Android's settings of the notification's channel, where the person turns it off. */
        fun channelSettingsIntent(context: Context): Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, AppNotificationKind.SYNC_CONNECTION.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

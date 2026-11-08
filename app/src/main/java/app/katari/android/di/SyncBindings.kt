package app.katari.android.di

import app.katari.android.sync.AndroidSyncBackgroundWork
import app.katari.android.sync.AndroidSyncNotifications
import app.katari.android.sync.AndroidThisDevice
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.trigger.SyncBackgroundWork
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
interface SyncBindings {
    @Binds
    val AndroidSyncBackgroundWork.syncBackgroundWork: SyncBackgroundWork

    @Binds
    val AndroidThisDevice.thisDevice: ThisDevice

    @Binds
    val AndroidSyncNotifications.syncNotifications: SyncNotifications
}

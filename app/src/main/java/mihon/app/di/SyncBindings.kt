package mihon.app.di

import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.trigger.SyncBackgroundWork
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.app.sync.AndroidSyncBackgroundWork
import mihon.app.sync.AndroidSyncNotifications
import mihon.app.sync.AndroidThisDevice

@BindingContainer
interface SyncBindings {
    @Binds
    val AndroidSyncBackgroundWork.syncBackgroundWork: SyncBackgroundWork

    @Binds
    val AndroidThisDevice.thisDevice: ThisDevice

    @Binds
    val AndroidSyncNotifications.syncNotifications: SyncNotifications
}

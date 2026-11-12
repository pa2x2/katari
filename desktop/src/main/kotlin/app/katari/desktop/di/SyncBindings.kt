package app.katari.desktop.di

import app.katari.desktop.sync.DesktopInstantSyncSetup
import app.katari.desktop.sync.DesktopSyncNotifications
import app.katari.desktop.sync.DesktopThisDevice
import app.katari.desktop.sync.InProcessSyncBackgroundWork
import app.katari.desktop.work.ResumableWork
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.trigger.SyncBackgroundWork
import app.katari.sync.ui.instant.InstantSyncSetup
import app.katari.sync.ui.notice.SyncNotifications
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet

@BindingContainer
interface SyncBindings {
    @Binds
    val InProcessSyncBackgroundWork.syncBackgroundWork: SyncBackgroundWork

    @Binds
    @IntoSet
    val InProcessSyncBackgroundWork.resumableSync: ResumableWork

    @Binds
    val DesktopThisDevice.thisDevice: ThisDevice

    @Binds
    val DesktopInstantSyncSetup.instantSyncSetup: InstantSyncSetup

    @Binds
    val DesktopSyncNotifications.syncNotifications: SyncNotifications
}

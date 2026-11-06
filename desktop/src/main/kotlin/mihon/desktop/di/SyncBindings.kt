package mihon.desktop.di

import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.client.trigger.SyncBackgroundWork
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import mihon.desktop.sync.DesktopThisDevice
import mihon.desktop.sync.InProcessSyncBackgroundWork
import mihon.desktop.work.ResumableWork

@BindingContainer
interface SyncBindings {
    @Binds
    val InProcessSyncBackgroundWork.syncBackgroundWork: SyncBackgroundWork

    @Binds
    @IntoSet
    val InProcessSyncBackgroundWork.resumableSync: ResumableWork

    @Binds
    val DesktopThisDevice.thisDevice: ThisDevice
}

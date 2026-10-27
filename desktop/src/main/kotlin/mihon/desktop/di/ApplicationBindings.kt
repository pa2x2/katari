package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.core.platform.PlatformContext
import mihon.desktop.application.ApplicationPlatformContext

@BindingContainer
interface ApplicationBindings {
    @Binds
    val ApplicationPlatformContext.platformContext: PlatformContext
}

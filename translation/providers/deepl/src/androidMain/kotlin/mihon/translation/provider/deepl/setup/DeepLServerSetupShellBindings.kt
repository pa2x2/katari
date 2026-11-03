package mihon.translation.provider.deepl.setup

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.translation.provider.server.setup.ProviderSetupShell

@ContributesTo(AppScope::class)
@BindingContainer
object DeepLServerSetupShellBindings {
    @Provides
    @IntoSet
    fun shell(): ProviderSetupShell = ProviderSetupShell(
        DeepLServerSetupScreen::class,
        DeepLServerSetupActivity::class.java,
    )
}

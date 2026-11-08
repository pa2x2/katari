package app.katari.translation.provider.deepl.setup

import app.katari.translation.provider.server.setup.ProviderSetupShell
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

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

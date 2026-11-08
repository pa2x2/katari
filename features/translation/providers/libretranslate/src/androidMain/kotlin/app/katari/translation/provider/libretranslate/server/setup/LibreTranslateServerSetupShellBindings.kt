package app.katari.translation.provider.libretranslate.server.setup

import app.katari.translation.provider.server.setup.ProviderSetupShell
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object LibreTranslateServerSetupShellBindings {
    @Provides
    @IntoSet
    fun shell(): ProviderSetupShell =
        ProviderSetupShell(LibreTranslateServerSetupScreen::class, LibreTranslateServerSetupActivity::class.java)
}

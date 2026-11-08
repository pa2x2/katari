package app.katari.translation.provider.server.di

import android.app.Application
import app.katari.translation.provider.server.AndroidKeystoreSecretStore
import app.katari.translation.provider.server.AndroidServerLocalNetworkGuard
import app.katari.translation.provider.server.SecretStore
import app.katari.translation.provider.server.ServerConnectionPreferenceStores
import app.katari.translation.provider.server.ServerLocalNetworkGuard
import app.katari.translation.provider.server.SharedPreferencesConnectionStores
import app.katari.translation.provider.server.setup.ActivityServerSetupDestination
import app.katari.translation.provider.server.setup.ProviderSetupShell
import app.katari.translation.provider.server.setup.ServerSetupDestination
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** Where Android keeps server connections and shows their setup. */
@ContributesTo(AppScope::class)
@BindingContainer
object AndroidServerConnectionBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun secretStore(application: Application): SecretStore = AndroidKeystoreSecretStore(application)

    @Provides
    @SingleIn(AppScope::class)
    fun preferenceStores(application: Application): ServerConnectionPreferenceStores =
        SharedPreferencesConnectionStores(application)

    @Provides
    fun localNetworkGuard(application: Application): ServerLocalNetworkGuard =
        AndroidServerLocalNetworkGuard(application)

    @Provides
    fun setupDestination(application: Application, shells: Set<ProviderSetupShell>): ServerSetupDestination =
        ActivityServerSetupDestination(application, shells)
}

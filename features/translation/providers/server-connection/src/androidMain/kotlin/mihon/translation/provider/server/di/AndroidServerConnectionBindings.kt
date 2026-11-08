package mihon.translation.provider.server.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.translation.provider.server.AndroidKeystoreSecretStore
import mihon.translation.provider.server.AndroidServerLocalNetworkGuard
import mihon.translation.provider.server.SecretStore
import mihon.translation.provider.server.ServerConnectionPreferenceStores
import mihon.translation.provider.server.ServerLocalNetworkGuard
import mihon.translation.provider.server.SharedPreferencesConnectionStores
import mihon.translation.provider.server.setup.ActivityServerSetupDestination
import mihon.translation.provider.server.setup.ProviderSetupShell
import mihon.translation.provider.server.setup.ServerSetupDestination

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

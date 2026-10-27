package mihon.script.runtime.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.NetworkHelper
import mihon.script.runtime.ScriptNetwork

@ContributesTo(AppScope::class)
@BindingContainer
object ScriptRuntimeBindings {
    @Provides
    fun network(
        network: NetworkHelper,
    ): ScriptNetwork = ScriptNetwork(network.client, network::defaultUserAgentProvider)
}

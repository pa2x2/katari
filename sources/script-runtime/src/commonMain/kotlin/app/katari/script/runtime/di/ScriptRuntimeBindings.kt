package app.katari.script.runtime.di

import app.katari.script.runtime.ScriptNetwork
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.NetworkHelper

@ContributesTo(AppScope::class)
@BindingContainer
object ScriptRuntimeBindings {
    @Provides
    fun network(
        network: NetworkHelper,
    ): ScriptNetwork = ScriptNetwork(network.client, network::defaultUserAgentProvider)
}

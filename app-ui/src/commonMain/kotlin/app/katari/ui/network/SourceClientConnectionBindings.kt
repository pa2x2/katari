package app.katari.ui.network

import app.katari.core.metro.AppCoroutineScope
import app.katari.core.metro.StartupWarmup
import app.katari.core.platform.network.NetworkConnectivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * The source client, and every client built from it, which share its connections, forgets its idle connections when
 * the network changes. One opened on the network before is dead once it changed, yet often nothing tells the client: a
 * request on it waits out the whole read timeout. Nor does the client see how old a connection is across the system's
 * sleep, whose time its clock doesn't count, so one left from before the sleep looks seconds old and is used again.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object SourceClientConnectionBindings {
    @Provides
    @IntoSet
    fun forgetConnectionsOnNetworkChange(
        networkHelper: NetworkHelper,
        connectivity: NetworkConnectivity,
        @AppCoroutineScope scope: CoroutineScope,
    ): StartupWarmup = StartupWarmup {
        scope.launch {
            connectivity.state()
                .distinctUntilChanged()
                .drop(1)
                .collect { networkHelper.client.connectionPool.evictAll() }
        }
    }
}

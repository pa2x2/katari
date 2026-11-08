package mihon.model.artifacts.runtime.network

import kotlinx.coroutines.flow.first
import mihon.core.platform.network.NetworkConnectivity

internal fun interface ModelArtifactNetworkPolicy {
    suspend fun isActiveNetworkMetered(): Boolean
}

/** Whether the active network is metered, as the platform judges it. */
internal class ConnectivityModelArtifactNetworkPolicy(
    private val connectivity: NetworkConnectivity,
) : ModelArtifactNetworkPolicy {
    override suspend fun isActiveNetworkMetered(): Boolean = connectivity.state().first().isMetered
}

package mihon.model.artifacts.runtime.network

import android.content.Context
import android.net.ConnectivityManager

internal fun interface ModelArtifactNetworkPolicy {
    fun isActiveNetworkMetered(): Boolean
}

internal class AndroidModelArtifactNetworkPolicy(
    private val context: Context,
) : ModelArtifactNetworkPolicy {
    override fun isActiveNetworkMetered(): Boolean =
        context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered ?: true
}

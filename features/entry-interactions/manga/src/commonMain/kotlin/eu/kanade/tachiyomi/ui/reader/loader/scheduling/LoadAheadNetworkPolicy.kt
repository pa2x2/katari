package eu.kanade.tachiyomi.ui.reader.loader.scheduling

import mihon.core.platform.network.NetworkConnectivity

/**
 * Whether an online chapter may load the whole chapter around the page being read. When the user limits loading
 * ahead to Wi-Fi, other networks only pay for the pages right around that page.
 */
internal class LoadAheadNetworkPolicy(
    private val connectivity: NetworkConnectivity,
) {
    fun allows(onlyOverWifi: Boolean): Boolean = !onlyOverWifi || connectivity.current().countsAsWifi
}

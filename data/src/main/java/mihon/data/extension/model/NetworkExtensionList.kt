package mihon.data.extension.model

import mihon.domain.extension.model.ExtensionStore
import eu.kanade.tachiyomi.extension.model.Extension as DomainExtension

/** A v2 index as one ecosystem's layout reads it. */
interface NetworkV2ExtensionStore : BaseNetworkExtensionStore {
    /** Null when the index only links to its list. */
    val extensionList: NetworkExtensionList?
}

/** A v2 extension list as one ecosystem's layout reads it. */
interface NetworkExtensionList {

    val isEmpty: Boolean

    /**
     * How many extensions read have what that ecosystem's extensions always have: a source, and an extension library
     * written its way. In a reading in the other ecosystem's layout, next to none do.
     */
    fun countFitting(): Int

    fun toAvailableExtensions(store: ExtensionStore): List<DomainExtension.Available>
}

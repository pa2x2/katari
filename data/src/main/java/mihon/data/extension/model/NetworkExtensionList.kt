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
     * Whether every extension read has what that ecosystem's extensions always have: a source, and an extension library
     * written its way. A reading in the other ecosystem's layout doesn't.
     */
    fun fitsLayout(): Boolean

    fun toAvailableExtensions(store: ExtensionStore): List<DomainExtension.Available>
}

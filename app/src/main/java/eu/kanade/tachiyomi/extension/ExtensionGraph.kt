package eu.kanade.tachiyomi.extension

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.extension.repository.ExtensionStoreRepository

/** Graph entry points of the extension loader, installer services and install activity, which can't be injected. */
interface ExtensionGraph {
    val extensionManager: ExtensionManager

    val trustExtension: TrustExtension

    val sourcePreferences: SourcePreferences

    val extensionStoreRepository: ExtensionStoreRepository
}

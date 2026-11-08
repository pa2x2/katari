package eu.kanade.domain.extension.interactor

import app.katari.domain.extension.model.ExtensionStore
import app.katari.domain.extension.repository.ExtensionStoreRepository
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.GlobalSourcePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import tachiyomi.core.common.preference.getAndSet

@Inject
class TrustExtension(
    private val preferences: GlobalSourcePreferences,
    private val repository: ExtensionStoreRepository,
) {

    fun isTrusted(
        pkgName: String,
        versionCode: Long,
        fingerprints: List<String>,
        stores: List<ExtensionStore>,
    ): Boolean {
        val trustedFingerprints = stores.mapNotNullTo(HashSet()) { it.signingKey }
        val key = "$pkgName:$versionCode:${fingerprints.last()}"
        return trustedFingerprints.any { fingerprints.contains(it) } || key in preferences.trustedExtensions.get()
    }

    /**
     * A file extension is trusted while the store it was installed from is added, since adding a store that doesn't
     * sign its extensions is what trusts them, or when the user trusted this very file.
     */
    fun isTrusted(pkgName: String, sha256: String, storeIndexUrl: String, stores: List<ExtensionStore>): Boolean {
        return stores.any { it.indexUrl == storeIndexUrl } ||
            "$pkgName:0:$sha256" in preferences.trustedExtensions.get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions.getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also { it += "$pkgName:$versionCode:$signatureHash" }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions.delete()
    }

    /**
     * Emits whenever what counts as trusted changes, either because a store was added or removed or
     * because an extension was trusted or had its trust revoked. Both sources replay their current
     * value, which is dropped.
     */
    fun changes(): Flow<Unit> {
        return merge(
            // Stores are rewritten whenever their index is refreshed, so only what vouches for extensions matters here:
            // a signing store's key, or an unsigned store itself
            repository.getAllAsFlow()
                .map { stores -> stores.mapTo(HashSet()) { it.signingKey ?: it.indexUrl } }
                .distinctUntilChanged()
                .drop(1),
            preferences.trustedExtensions.changes()
                .distinctUntilChanged()
                .drop(1),
        )
            .map {}
    }
}

package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.kind.compareLibVersions
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ExtensionStore

/*
 * Several stores can list the same package. Android only replaces an installed apk with one signed by the same key,
 * so a store's listing is only installable over an extension signed with that store's key.
 */

private val byVersion = compareBy<Extension.Available> { it.versionCode }
    .then { a, b -> compareLibVersions(a.versionName, b.versionName) ?: 0 }

/**
 * The newest listing of this extension among the stores whose signing key it's signed with.
 */
internal fun Extension.Installed.findListing(available: Collection<Extension.Available>): Extension.Available? {
    return available
        .filter { it.pkgName == pkgName && it.store.signingKey in signatures }
        .maxWithOrNull(byVersion)
}

/**
 * Stores sharing a signing key serve the same apks, so only the newest of their listings is worth offering. Stores
 * with different keys offer different apks, each installable where that key's apk isn't already installed.
 */
internal fun Collection<Extension.Available>.newestListingPerSigningKey(): List<Extension.Available> {
    return groupBy { it.pkgName to it.store.signingKey }.values.map { listings -> listings.maxWith(byVersion) }
}

/** The newest listing of this extension that can be installed over it, when that's newer than it. */
internal fun Extension.Installed.findUpdate(available: Collection<Extension.Available>): Extension.Available? {
    return available
        .filter { it.pkgName == pkgName && it.store.signingKey in signatures && it.isInstallable }
        .maxWithOrNull(byVersion)
        ?.takeIf { listing ->
            val hasNewerLib = compareLibVersions(listing.versionName, versionName)?.let { it > 0 } == true
            listing.versionCode > versionCode || hasNewerLib
        }
}

/**
 * The store this extension comes from among those whose signing key it's signed with. Several stores can share a key,
 * so one that lists the extension names where it comes from better than whichever of them was added first.
 */
internal fun Extension.Installed.pickStore(
    stores: List<ExtensionStore>,
    available: Collection<Extension.Available>,
): ExtensionStore? {
    val signingStores = stores.filter { it.signingKey in signatures }
    val listedBy = available.filter { it.pkgName == pkgName }.mapTo(HashSet()) { it.store.indexUrl }
    return signingStores.firstOrNull { it.indexUrl in listedBy } ?: signingStores.firstOrNull()
}

/**
 * Fails unless [apkSignatures], those of the apk downloaded for this listing, include its store's key. The store's
 * index names the apk, but nothing else ties what's served there to the store's key.
 */
internal fun Extension.Available.requireSignedByStore(apkSignatures: List<String>) {
    check(store.signingKey != ExtensionStore.NO_SIGNING_KEY) {
        "The signing key of ${store.name} is unknown until the store is refreshed"
    }
    check(store.signingKey in apkSignatures) { "The extension isn't signed with the key of ${store.name}" }
}

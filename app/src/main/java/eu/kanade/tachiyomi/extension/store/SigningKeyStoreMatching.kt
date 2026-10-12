package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import mihon.domain.extension.model.ExtensionStore

/*
 * Several stores can list the same package. Android only replaces an installed apk with one signed by the same key,
 * so a store's listing is only installable over an extension signed with that store's key.
 */

private val byVersion = compareBy<Extension.Available> { it.versionCode }
    .then { a, b -> ExtensionLoader.compareLibVersions(a.versionName, b.versionName) ?: 0 }

/**
 * The newest listing of this extension among the stores whose signing key it's signed with.
 */
internal fun Extension.Installed.findListing(available: Collection<Extension.Available>): Extension.Available? {
    return available
        .filter { it.pkgName == pkgName && it.store.signingKey in signatures }
        .maxWithOrNull(byVersion)
}

internal fun Extension.Installed.findUpdate(available: Collection<Extension.Available>): Extension.Available? {
    return findListing(available)?.takeIf { listing ->
        val hasNewerLib = ExtensionLoader.compareLibVersions(listing.versionName, versionName)?.let { it > 0 } == true
        listing.versionCode > versionCode || hasNewerLib
    }
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

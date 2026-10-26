package eu.kanade.tachiyomi.extension.store

import eu.kanade.tachiyomi.extension.kind.ExtensionKinds
import eu.kanade.tachiyomi.extension.kind.compareLibVersions
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstalledForm
import mihon.domain.extension.model.ExtensionStore

/*
 * Several stores can list the same extension. Android only replaces an installed apk with one signed by the same key,
 * so a store's listing is only installable over an extension signed with that store's key. A file extension is
 * updated only from the store it was installed from, which is what vouched for it.
 */

private val byVersion = Comparator<Extension.Available> { a, b ->
    val fileKind = ExtensionKinds.ofFilePkgName(a.pkgName)?.first
    if (fileKind != null) {
        fileKind.compareVersions(a.versionName, b.versionName)
    } else {
        compareValuesBy(a, b) { it.versionCode }.takeIf { it != 0 }
            ?: compareLibVersions(a.versionName, b.versionName)
            ?: 0
    }
}

/** Whether [store]'s listings are of this very extension, as opposed to another build under the same name. */
internal fun Extension.Installed.isServedBy(store: ExtensionStore): Boolean {
    return when (val form = form) {
        is InstalledForm.Package -> store.signingKey in form.signatures
        is InstalledForm.File -> store.indexUrl == form.storeIndexUrl
    }
}

/** The newest listing of this extension among the stores serving it. */
internal fun Extension.Installed.findListing(available: Collection<Extension.Available>): Extension.Available? {
    return available
        .filter { it.pkgName == pkgName && isServedBy(it.store) }
        .maxWithOrNull(byVersion)
}

/**
 * Stores sharing a signing key serve the same apks, so only the newest of their listings is worth offering. Stores
 * with different keys offer different apks, each installable where that key's apk isn't already installed. Stores that
 * don't sign their extensions each vouch for their own.
 */
internal fun Collection<Extension.Available>.newestListingPerOrigin(): List<Extension.Available> {
    return groupBy { it.pkgName to (it.store.signingKey ?: it.store.indexUrl) }
        .values
        .map { listings -> listings.maxWith(byVersion) }
}

/** The newest listing of this extension that can be installed over it, when that's newer than it. */
internal fun Extension.Installed.findUpdate(available: Collection<Extension.Available>): Extension.Available? {
    return available
        .filter { it.pkgName == pkgName && isServedBy(it.store) && it.isInstallable }
        .maxWithOrNull(byVersion)
        ?.takeIf(::isOlderThan)
}

private fun Extension.Installed.isOlderThan(listing: Extension.Available): Boolean {
    val fileKind = ExtensionKinds.ofFilePkgName(pkgName)?.first
    if (fileKind != null) return fileKind.compareVersions(listing.versionName, versionName) > 0
    val hasNewerLib = compareLibVersions(listing.versionName, versionName)?.let { it > 0 } == true
    return listing.versionCode > versionCode || hasNewerLib
}

/**
 * The store this extension comes from among those serving it. Several stores can share a key, so one that lists the
 * extension names where it comes from better than whichever of them was added first.
 */
internal fun Extension.Installed.pickStore(
    stores: List<ExtensionStore>,
    available: Collection<Extension.Available>,
): ExtensionStore? {
    val servingStores = stores.filter(::isServedBy)
    val listedBy = available.filter { it.pkgName == pkgName }.mapTo(HashSet()) { it.store.indexUrl }
    return servingStores.firstOrNull { it.indexUrl in listedBy } ?: servingStores.firstOrNull()
}

/**
 * Fails unless [apkSignatures], those of the apk downloaded for this listing, include its store's key. The store's
 * index names the apk, but nothing else ties what's served there to the store's key.
 */
internal fun Extension.Available.requireSignedByStore(apkSignatures: List<String>) {
    checkNotNull(store.signingKey) { "${store.name} doesn't sign its extensions" }
    check(store.signingKey != ExtensionStore.NO_SIGNING_KEY) {
        "The signing key of ${store.name} is unknown until the store is refreshed"
    }
    check(store.signingKey in apkSignatures) { "The extension isn't signed with the key of ${store.name}" }
}

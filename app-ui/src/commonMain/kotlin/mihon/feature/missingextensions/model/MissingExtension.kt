package mihon.feature.missingextensions.model

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * An extension library entries need that isn't installed, or is installed but not trusted in this app. Known ones are
 * one per extension, however many of its sources the entries use; a source nothing ties to an extension is one of its
 * own.
 */
data class MissingExtension(
    /** The extension's package name, or the source's key for a source nothing ties to an extension. */
    val key: String,
    val name: String,
    /** Null when no added store lists it. */
    val listing: Extension.Available?,
    /**
     * The extension, when it's installed on the device but not trusted in this app: trusting it loads it, with no
     * install. Extensions are installed for the whole device while each app trusts them on its own.
     */
    val untrusted: Extension.NotLoaded?,
    /** Null when it can be installed from [listing], or trusted. */
    val cantInstall: CantInstall?,
    /** How many library entries each profile has from it, by profile ID. */
    val countsByProfile: Map<Long, Long>,
) {
    val titleCount: Long
        get() = countsByProfile.values.sum()

    val isInstallable: Boolean
        get() = untrusted == null && cantInstall == null
}

/**
 * A source entries come from that isn't installed, with the listing that would install it, if a store lists it, and the
 * installed extension that would provide it once trusted, if there is one.
 */
data class NeededSource(
    /** Tells sources nothing ties to an extension apart. */
    val key: Long,
    val name: String,
    val listing: Extension.Available?,
    val untrusted: Extension.NotLoaded?,
    /** How many library entries each profile has from it, by profile ID. */
    val countsByProfile: Map<Long, Long>,
)

/** [sources] grouped by the extension that provides each, most titles first. */
fun missingExtensions(
    sources: List<NeededSource>,
    cantInstall: (Extension.Available) -> CantInstall?,
): List<MissingExtension> = sources
    .groupBy { it.untrusted?.pkgName ?: it.listing?.pkgName ?: "source:${it.key}" }
    .map { (key, grouped) ->
        val listing = grouped.firstNotNullOfOrNull { it.listing }
        val untrusted = grouped.firstNotNullOfOrNull { it.untrusted }
        MissingExtension(
            key = key,
            name = untrusted?.name ?: listing?.name ?: grouped.first().name,
            listing = listing,
            untrusted = untrusted,
            cantInstall = when {
                untrusted != null -> null
                listing == null -> CantInstall.NOT_LISTED
                else -> cantInstall(listing)
            },
            countsByProfile = grouped
                .flatMap { it.countsByProfile.entries }
                .groupingBy { it.key }
                .fold(0L) { total, entry -> total + entry.value },
        )
    }
    .sortedByDescending { it.titleCount }

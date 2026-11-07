package mihon.feature.missingextensions.model

import eu.kanade.tachiyomi.extension.model.Extension

/**
 * An extension library entries need that isn't installed. Listed ones are one per extension, however many of its
 * sources the entries use; a source no store lists is one of its own, since nothing says which extension it was.
 */
data class MissingExtension(
    /** The listing's package name, or the source's key for a source no store lists. */
    val key: String,
    val name: String,
    /** Null when no added store lists it. */
    val listing: Extension.Available?,
    /** Null when it can be installed from [listing]. */
    val cantInstall: CantInstall?,
    /** How many library entries each profile has from it, by profile ID. */
    val countsByProfile: Map<Long, Long>,
) {
    val titleCount: Long
        get() = countsByProfile.values.sum()
}

/** A source entries come from that isn't installed, with the listing that would install it, if a store lists it. */
data class NeededSource(
    /** Tells sources no store lists apart. */
    val key: Long,
    val name: String,
    val listing: Extension.Available?,
    /** How many library entries each profile has from it, by profile ID. */
    val countsByProfile: Map<Long, Long>,
)

/** [sources] grouped by the extension that provides each, most titles first. */
fun missingExtensions(
    sources: List<NeededSource>,
    cantInstall: (Extension.Available) -> CantInstall?,
): List<MissingExtension> = sources
    .groupBy { it.listing?.pkgName ?: "source:${it.key}" }
    .map { (key, grouped) ->
        val listing = grouped.first().listing
        MissingExtension(
            key = key,
            name = listing?.name ?: grouped.first().name,
            listing = listing,
            cantInstall = if (listing == null) CantInstall.NOT_LISTED else cantInstall(listing),
            countsByProfile = grouped
                .flatMap { it.countsByProfile.entries }
                .groupingBy { it.key }
                .fold(0L) { total, entry -> total + entry.value },
        )
    }
    .sortedByDescending { it.titleCount }

package mihon.extension.smoke.run

import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
import eu.kanade.tachiyomi.extension.model.Extension
import mihon.desktop.extension.ExtensionHostGraph
import mihon.extension.smoke.options.SmokeOptions
import mihon.extension.smoke.results.ExtensionResult
import mihon.extension.smoke.results.InstallMeasurements
import mihon.extension.smoke.results.Outcome
import mihon.extension.smoke.results.OutcomeKind
import mihon.extension.smoke.results.SourceResult

/** A store's listings split by what the run does with them. */
internal data class StoreSelection(
    val toInstall: List<Extension.Available>,
    /** Listings counted without installing: of a family desktop doesn't host yet, or that the app can't run. */
    val counted: List<ExtensionResult>,
    /** Listings the filters and limits left out. */
    val leftOut: Int,
)

/**
 * Which listings of a store the run installs. The filters apply to every listing; the store's limit only to those it
 * would install, sampled evenly over the store's list by package name, so the same limit picks the same extensions
 * again and runs compare like with like.
 */
internal class ListingSelection(private val options: SmokeOptions, private val host: ExtensionHostGraph) {

    fun select(store: ListedStore): StoreSelection {
        val matching = store.listings
            .filter { listing -> options.extensions.isEmpty() || options.extensions.any { listing.matches(it) } }
            .filter { listing ->
                options.families.isEmpty() || host.extensionKinds.familyOf(listing) in options.families
            }
        val counted = mutableListOf<ExtensionResult>()
        val installable = mutableListOf<Extension.Available>()
        for (listing in matching) {
            val family = host.extensionKinds.familyOf(listing)
            when {
                family == null -> counted += listing.toResult(
                    store.url,
                    null,
                    Outcome(OutcomeKind.NOT_INSTALLABLE, "No family's library is ${listing.libVersionName}"),
                )
                family !in host.hostedExtensionFamilies.families -> counted += listing.toResult(
                    store.url,
                    family,
                    Outcome(OutcomeKind.FAMILY_NOT_ENABLED, "${family.name} extensions aren't hosted on desktop yet"),
                )
                !host.extensionCompatibility.isInstallable(listing) -> counted += listing.toResult(
                    store.url,
                    family,
                    Outcome(
                        OutcomeKind.NOT_INSTALLABLE,
                        if (listing.isTorrent) "Torrent extension" else "Library ${listing.libVersionName}",
                    ),
                )
                else -> installable += listing
            }
        }
        val sampled = installable.sortedBy { it.pkgName }.sampled(options.maxPerStore.forStore(store.url))
        return StoreSelection(
            toInstall = sampled,
            counted = counted,
            leftOut = store.listings.size - matching.size + installable.size - sampled.size,
        )
    }

    private fun Extension.Available.matches(text: String): Boolean =
        pkgName.contains(text, ignoreCase = true) || name.contains(text, ignoreCase = true)

    private fun <T> List<T>.sampled(limit: Int?): List<T> {
        if (limit == null || size <= limit) return this
        return List(limit) { index -> this[index * size / limit] }
    }
}

internal fun Extension.Available.toResult(
    store: String,
    family: ExtensionFamily?,
    outcome: Outcome,
    install: InstallMeasurements? = null,
    sources: List<SourceResult> = emptyList(),
) = ExtensionResult(
    store = store,
    pkgName = pkgName,
    name = name,
    versionName = versionName,
    libVersion = libVersionName,
    family = family.reportName,
    outcome = outcome,
    install = install,
    sources = sources,
)

internal val ExtensionFamily?.reportName: String get() = this?.name ?: "UNKNOWN"

package eu.kanade.tachiyomi.extension.kind

import android.content.pm.PackageInfo
import android.os.Bundle
import eu.kanade.tachiyomi.extension.aniyomi.AniyomiExtensionKind
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * The extension kinds Katari loads. Kinds sharing a feature tell their packages apart by the extension library the
 * package declares: the first that runs it loads the package.
 */
internal object ExtensionKinds {

    private val all: List<ExtensionKind> = listOf(
        TachiyomiLibExtensionKind,
        EntrySdkExtensionKind,
        AniyomiExtensionKind,
    )

    fun isExtension(pkgInfo: PackageInfo): Boolean = candidates(pkgInfo).isNotEmpty()

    /**
     * The kind that loads the package and how it describes it. When none runs its library, the description of the kind
     * it was built for, or else the first candidate's, which says so in its refusal. `null` for a package that isn't an
     * extension.
     */
    fun describe(
        pkgInfo: PackageInfo,
        versionName: String,
        metaData: Bundle,
        label: () -> String,
    ): Pair<ExtensionKind, ExtensionDescription>? {
        val described = candidates(pkgInfo).map { kind ->
            kind to kind.describe(pkgInfo.packageName, versionName, metaData, label)
        }
        return described.firstOrNull { (_, description) -> description.refusal == null }
            ?: described.firstOrNull { (kind, description) ->
                description.libVersionName?.let(kind::isBuiltFor) == true
            }
            ?: described.firstOrNull()
    }

    /** Whether a kind of the listing's ecosystem runs the library it declares. */
    fun runsListedLib(sourceIdScheme: SourceIdScheme, libVersionName: String): Boolean {
        return all.any { it.sourceIdScheme == sourceIdScheme && it.runsLib(libVersionName) }
    }

    /** The kind of the listing's ecosystem whose library it declares, `null` when none's is. */
    fun forListing(sourceIdScheme: SourceIdScheme, libVersionName: String): ExtensionKind? {
        return all.firstOrNull { it.sourceIdScheme == sourceIdScheme && it.isBuiltFor(libVersionName) }
    }

    fun ofProvider(providerKind: SourceProviderKind): ExtensionKind = all.first { it.providerKind == providerKind }

    private fun candidates(pkgInfo: PackageInfo): List<ExtensionKind> {
        val features = pkgInfo.reqFeatures.orEmpty().mapNotNullTo(HashSet()) { it.name }
        return all.filter { it.feature in features }
    }
}

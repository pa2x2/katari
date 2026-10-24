package eu.kanade.tachiyomi.extension.kind

import android.content.pm.PackageInfo
import android.os.Bundle

/**
 * The extension kinds Katari loads. Kinds sharing a feature tell their packages apart by the extension library the
 * package declares: the first that runs it loads the package.
 */
internal object ExtensionKinds {

    private val all: List<ExtensionKind> = listOf(
        TachiyomiLibExtensionKind,
        EntrySdkExtensionKind,
    )

    fun isExtension(pkgInfo: PackageInfo): Boolean = candidates(pkgInfo).isNotEmpty()

    /**
     * The kind that loads the package and how it describes it. When none runs its library, the first candidate's
     * description, which says so in its refusal. `null` for a package that isn't an extension.
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
        return described.firstOrNull { (_, description) -> description.refusal == null } ?: described.firstOrNull()
    }

    /** Whether any kind runs the library a store listing declares. */
    fun runsListedLib(libVersionName: String): Boolean = all.any { it.runsLib(libVersionName) }

    private fun candidates(pkgInfo: PackageInfo): List<ExtensionKind> {
        val features = pkgInfo.reqFeatures.orEmpty().mapNotNullTo(HashSet()) { it.name }
        return all.filter { it.feature in features }
    }
}

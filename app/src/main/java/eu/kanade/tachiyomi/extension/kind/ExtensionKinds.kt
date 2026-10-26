package eu.kanade.tachiyomi.extension.kind

import android.content.pm.PackageInfo
import android.os.Bundle
import eu.kanade.tachiyomi.extension.aniyomi.AniyomiExtensionKind
import eu.kanade.tachiyomi.extension.file.FileExtensionKind
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * The extension kinds Katari loads. Package kinds sharing a feature tell their packages apart by the extension library
 * the package declares: the first that runs it loads the package.
 */
internal object ExtensionKinds {

    private val packageKinds: List<PackageExtensionKind> = listOf(
        TachiyomiLibExtensionKind,
        EntrySdkExtensionKind,
        AniyomiExtensionKind,
    )

    val fileKinds: List<FileExtensionKind> = listOf()

    private val all: List<ExtensionKind> = packageKinds + fileKinds

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
    ): Pair<PackageExtensionKind, ExtensionDescription>? {
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

    /** The file kind of an installed extension's package name (`directoryName:id`) and its ID; `null` for an apk. */
    fun ofFilePkgName(pkgName: String): Pair<FileExtensionKind, String>? {
        val directoryName = pkgName.substringBefore(':', missingDelimiterValue = "")
        val kind = fileKinds.firstOrNull { it.directoryName == directoryName } ?: return null
        return kind to pkgName.substringAfter(':')
    }

    /** The file kind of a listing's ecosystem, `null` when its extensions are apks. */
    fun fileKindOf(sourceIdScheme: SourceIdScheme): FileExtensionKind? {
        return fileKinds.firstOrNull { it.sourceIdScheme == sourceIdScheme }
    }

    private fun candidates(pkgInfo: PackageInfo): List<PackageExtensionKind> {
        val features = pkgInfo.reqFeatures.orEmpty().mapNotNullTo(HashSet()) { it.name }
        return packageKinds.filter { it.feature in features }
    }
}

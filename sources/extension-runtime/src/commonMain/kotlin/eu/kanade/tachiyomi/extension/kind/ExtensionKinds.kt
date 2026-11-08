package eu.kanade.tachiyomi.extension.kind

import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.extension.aniyomi.AniyomiExtensionKind
import eu.kanade.tachiyomi.extension.file.FileExtensionKind
import eu.kanade.tachiyomi.extension.lnreader.LnReaderExtensionKind
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies
import eu.kanade.tachiyomi.source.lnreader.LnReaderSourceFactory
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * The extension kinds Katari loads. Package kinds sharing a feature tell their packages apart by the extension library
 * the package declares: the first that runs it loads the package.
 */
@Inject
@SingleIn(AppScope::class)
class ExtensionKinds(
    lnReaderSourceFactory: () -> LnReaderSourceFactory,
    profileStore: ProfileStore,
    private val hosted: HostedExtensionFamilies,
) {

    private val packageKinds: List<PackageExtensionKind> = listOf(
        TachiyomiLibExtensionKind,
        EntrySdkExtensionKind,
        AniyomiExtensionKind,
    )

    internal val fileKinds: List<FileExtensionKind> = listOf(LnReaderExtensionKind(lnReaderSourceFactory, profileStore))

    private val all: List<ExtensionKind> = packageKinds + fileKinds

    fun isExtension(extensionPackage: ExtensionPackage): Boolean = candidates(extensionPackage).isNotEmpty()

    /**
     * The kind that loads the package and how it describes it. When none runs its library, the description of the kind
     * it was built for, or else the first candidate's, which says so in its refusal. `null` for a package that isn't an
     * extension.
     */
    internal fun describe(
        extensionPackage: ExtensionPackage,
        versionName: String,
        metaData: ExtensionMetaData,
        label: () -> String,
    ): Pair<PackageExtensionKind, ExtensionDescription>? {
        val described = candidates(extensionPackage).map { kind ->
            kind to kind.describe(extensionPackage.packageName, versionName, metaData, label)
        }
        return described.firstOrNull { (_, description) -> description.refusal == null }
            ?: described.firstOrNull { (kind, description) ->
                description.libVersionName?.let(kind::isBuiltFor) == true
            }
            ?: described.firstOrNull()
    }

    /** Whether a kind of the listing's ecosystem the platform hosts runs the library it declares. */
    internal fun runsListedLib(sourceIdScheme: SourceIdScheme, libVersionName: String): Boolean {
        return all.any {
            it.family in hosted.families && it.sourceIdScheme == sourceIdScheme && it.runsLib(libVersionName)
        }
    }

    /** The family whose library [listing] declares, hosted or not; `null` when no kind's library is the one it names. */
    fun familyOf(listing: Extension.Available): ExtensionFamily? =
        forListing(listing.sourceIdScheme, listing.libVersionName)?.family

    /** The kind of the listing's ecosystem whose library it declares, `null` when none's is. */
    internal fun forListing(sourceIdScheme: SourceIdScheme, libVersionName: String): ExtensionKind? {
        return all.firstOrNull { it.sourceIdScheme == sourceIdScheme && it.isBuiltFor(libVersionName) }
    }

    internal fun ofProvider(providerKind: SourceProviderKind): ExtensionKind =
        all.first { it.providerKind == providerKind }

    /** The file kind of an installed extension's package name (`directoryName:id`) and its ID; `null` for an apk. */
    internal fun ofFilePkgName(pkgName: String): Pair<FileExtensionKind, String>? {
        val directoryName = pkgName.substringBefore(':', missingDelimiterValue = "")
        val kind = fileKinds.firstOrNull { it.directoryName == directoryName } ?: return null
        return kind to pkgName.substringAfter(':')
    }

    /** The file kind of a listing's ecosystem, `null` when its extensions are apks. */
    internal fun fileKindOf(sourceIdScheme: SourceIdScheme): FileExtensionKind? {
        return fileKinds.firstOrNull { it.sourceIdScheme == sourceIdScheme }
    }

    private fun candidates(extensionPackage: ExtensionPackage): List<PackageExtensionKind> {
        val features = extensionPackage.features
        return packageKinds.filter { it.feature in features }
    }
}

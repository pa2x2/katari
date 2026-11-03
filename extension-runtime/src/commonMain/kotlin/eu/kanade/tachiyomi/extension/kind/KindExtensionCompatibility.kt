package eu.kanade.tachiyomi.extension.kind

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.platform.HostedExtensionFamilies
import eu.kanade.tachiyomi.extension.store.isInstallable
import tachiyomi.i18n.*

/** What the extension kinds of the families the platform hosts run, as `app/extension-compatibility.json` lists them. */
@Inject
@ContributesBinding(AppScope::class)
class KindExtensionCompatibility(
    private val kinds: ExtensionKinds,
    hosted: HostedExtensionFamilies,
) : ExtensionCompatibility {
    override val families: List<ExtensionFamilySupport> = ExtensionFamily.entries
        .filter { it in hosted.families }
        .map { it.support }

    override fun isInstallable(listing: Extension.Available): Boolean = listing.isInstallable(kinds)

    override fun unsupportedSdk(extension: Extension): UnsupportedSdk? = when (extension) {
        is Extension.NotLoaded -> (extension.reason as? Extension.NotLoaded.Reason.UnsupportedLibVersion)?.let {
            kinds.ofProvider(it.providerKind).unsupportedSdk(it.libVersionName)
        }
        is Extension.Available -> kinds.forListing(extension.sourceIdScheme, extension.libVersionName)
            ?.takeUnless { it.runsLib(extension.libVersionName) }
            ?.unsupportedSdk(extension.libVersionName)
        is Extension.Loaded -> null
    }
}

private fun ExtensionKind.unsupportedSdk(libVersionName: String): UnsupportedSdk {
    return UnsupportedSdk(family.support, libVersionName, isNewerThanRun(libVersionName))
}

private val ExtensionFamily.support: ExtensionFamilySupport
    get() = ExtensionFamilySupport(
        title = when (this) {
            ExtensionFamily.KATARI -> MR.strings.ext_family_katari
            ExtensionFamily.TACHIYOMI -> MR.strings.ext_family_tachiyomi
            ExtensionFamily.ANIYOMI -> MR.strings.ext_family_aniyomi
            ExtensionFamily.LNREADER -> MR.strings.ext_family_lnreader
        },
        library = library,
        libVersions = libVersions,
    )

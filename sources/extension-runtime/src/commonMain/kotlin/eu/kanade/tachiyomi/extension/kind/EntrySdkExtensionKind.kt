package eu.kanade.tachiyomi.extension.kind

import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/** Extensions built against Katari's Entry SDK. */
internal object EntrySdkExtensionKind : PackageExtensionKind {

    // Entry source API extensions exchange Kotlin types with the app; a bundled Kotlin runtime would load as distinct
    // classes and fail across that boundary with LinkageError or ClassCastException.
    private val PARENT_FIRST_PACKAGES = listOf("kotlin.")

    override val feature = TACHIYOMI_EXTENSION_FEATURE

    // Shared with Tachiyomi extensions so a port can take over a Keiyoushi source's ID
    override val sourceIdScheme = SourceIdScheme.TACHIYOMI
    override val providerKind = SourceProviderKind.KATARI_EXTENSION
    override val family = ExtensionFamily.KATARI

    override fun runsLib(libVersionName: String): Boolean = family.runsLibVersion(libVersionName)

    // Tachiyomi's extensions-lib never left 1.x
    override fun isBuiltFor(libVersionName: String): Boolean = (LibVersion.parse(libVersionName)?.major ?: 0) >= 2

    override fun isNewerThanRun(libVersionName: String): Boolean? = family.isNewerThanListed(libVersionName)

    override fun describe(packageName: String, versionName: String, metaData: ExtensionMetaData, label: () -> String) =
        describeTachiyomiPackage(packageName, versionName, metaData, label, this)

    override suspend fun createSources(
        extensionPackage: ExtensionPackage,
        classLoaders: ExtensionClassLoaders,
        description: ExtensionDescription,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource> {
        val classLoader = classLoaders.classLoader(
            extensionPackage = extensionPackage,
            parentFirstPackagePrefixes = PARENT_FIRST_PACKAGES,
            prefersPlatformLoader = false,
        )
        return createTachiyomiSources(description.sourceClassNames, classLoader)
    }
}

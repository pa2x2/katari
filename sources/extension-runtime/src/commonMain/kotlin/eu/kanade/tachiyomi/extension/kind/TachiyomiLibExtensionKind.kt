package eu.kanade.tachiyomi.extension.kind

import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/** Extensions built against the Tachiyomi extensions-lib, as Keiyoushi publishes them. */
internal object TachiyomiLibExtensionKind : PackageExtensionKind {

    override val feature = TACHIYOMI_EXTENSION_FEATURE
    override val sourceIdScheme = SourceIdScheme.TACHIYOMI
    override val providerKind = SourceProviderKind.TACHIYOMI_EXTENSION
    override val family = ExtensionFamily.TACHIYOMI

    override fun runsLib(libVersionName: String): Boolean = family.runsLibVersion(libVersionName)

    // Katari's Entry SDK started at 2.0
    override fun isBuiltFor(libVersionName: String): Boolean = LibVersion.parse(libVersionName)?.major == 1

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
            parentFirstPackagePrefixes = emptyList(),
            prefersPlatformLoader = true,
        )
        return createTachiyomiSources(description.sourceClassNames, classLoader)
    }
}

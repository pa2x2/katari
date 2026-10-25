package eu.kanade.tachiyomi.extension.kind

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import eu.kanade.tachiyomi.extension.util.DelegateLastPathClassLoader
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/** Extensions built against Katari's Entry SDK. */
internal object EntrySdkExtensionKind : ExtensionKind {

    private val LIB_VERSION_FAMILY_RANGE = LibVersion.range("3.0", "3.0")

    // Entry source API extensions exchange Kotlin types with the app; a bundled Kotlin runtime would load as distinct
    // classes and fail across that boundary with LinkageError or ClassCastException.
    private val PARENT_FIRST_PACKAGES = listOf("kotlin.")

    override val feature = TACHIYOMI_EXTENSION_FEATURE

    // Shared with Tachiyomi extensions so a port can take over a Keiyoushi source's ID
    override val sourceIdScheme = SourceIdScheme.TACHIYOMI
    override val providerKind = SourceProviderKind.KATARI_EXTENSION

    override fun runsLib(libVersionName: String): Boolean {
        val libVersion = LibVersion.parse(libVersionName) ?: return false
        return libVersion.withoutPatch() in LIB_VERSION_FAMILY_RANGE
    }

    override fun describe(packageName: String, versionName: String, metaData: Bundle, label: () -> String) =
        describeTachiyomiPackage(packageName, versionName, metaData, label, ::runsLib)

    override suspend fun createSources(
        context: Context,
        appInfo: ApplicationInfo,
        packageName: String,
        description: ExtensionDescription,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource> {
        val classLoader = DelegateLastPathClassLoader(
            dexPath = appInfo.sourceDir,
            librarySearchPath = null,
            parent = context.classLoader,
            parentFirstPackagePrefixes = PARENT_FIRST_PACKAGES,
        )
        return createTachiyomiSources(description.sourceClassNames, classLoader)
    }
}

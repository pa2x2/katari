package eu.kanade.tachiyomi.extension.aniyomi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.extension.kind.ExtensionDescription
import eu.kanade.tachiyomi.extension.kind.ExtensionKind
import eu.kanade.tachiyomi.extension.kind.LoadPassSourceKeys
import eu.kanade.tachiyomi.extension.util.DelegateLastPathClassLoader
import eu.kanade.tachiyomi.source.aniyomi.toUnifiedSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.aniyomi.runtime.AniyomiExtensionClassLoading
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * Extensions built against Aniyomi's extensions-lib, as Yūzōnō publishes them. Their sources run through the Aniyomi
 * runtime and the adapter, which wraps each under the key its `aniyomi` identity was given, since an Aniyomi ID can
 * repeat a Tachiyomi source's.
 */
internal object AniyomiExtensionKind : ExtensionKind {

    // The libs Aniyomi itself loads
    private val LIB_VERSIONS = setOf(14.0, 16.0, 17.0)

    override val feature = ANIYOMI_EXTENSION_FEATURE
    override val sourceIdScheme = SourceIdScheme.ANIYOMI
    override val providerKind = SourceProviderKind.ANIYOMI_EXTENSION

    override fun runsLib(libVersionName: String): Boolean = libVersionName.toDoubleOrNull() in LIB_VERSIONS

    override fun describe(packageName: String, versionName: String, metaData: Bundle, label: () -> String) =
        describeAniyomiPackage(packageName, versionName, metaData, label, ::runsLib)

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
            parentFirstPackagePrefixes = AniyomiExtensionClassLoading.parentFirstPackagePrefixes,
        )
        val sources = description.sourceClassNames.flatMap { sourceClass ->
            when (val obj = Class.forName(sourceClass, false, classLoader).getDeclaredConstructor().newInstance()) {
                is AnimeSource -> listOf(obj)
                is AnimeSourceFactory -> obj.createSources()
                else -> throw Exception("Unknown source class type: ${obj.javaClass}")
            }
        }
        return sources.map { it.toUnifiedSource(keys.keyOf(SourceIdentity(sourceIdScheme, it.id))) }
    }
}

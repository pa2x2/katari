package eu.kanade.tachiyomi.extension.aniyomi

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import eu.kanade.tachiyomi.extension.kind.ExtensionDescription
import eu.kanade.tachiyomi.extension.kind.ExtensionFamily
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

    override val feature = ANIYOMI_EXTENSION_FEATURE
    override val sourceIdScheme = SourceIdScheme.ANIYOMI
    override val providerKind = SourceProviderKind.ANIYOMI_EXTENSION
    override val family = ExtensionFamily.ANIYOMI

    // Aniyomi numbers its libs with whole numbers, which manifests may write as 14 or 14.0
    private val libVersions by lazy { family.libVersions.map(String::toDouble) }

    override fun runsLib(libVersionName: String): Boolean = libVersionName.toDoubleOrNull() in libVersions

    // The only kind that loads its feature
    override fun isBuiltFor(libVersionName: String): Boolean = true

    override fun isNewerThanRun(libVersionName: String): Boolean? =
        libVersionName.toDoubleOrNull()?.let { it > libVersions.max() }

    override fun describe(packageName: String, versionName: String, metaData: Bundle, label: () -> String) =
        describeAniyomiPackage(packageName, versionName, metaData, label, this)

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

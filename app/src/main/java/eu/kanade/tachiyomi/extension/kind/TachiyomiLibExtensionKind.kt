package eu.kanade.tachiyomi.extension.kind

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import dalvik.system.DelegateLastClassLoader
import eu.kanade.tachiyomi.extension.util.DelegateLastPathClassLoader
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind
import java.io.File

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

    override fun describe(packageName: String, versionName: String, metaData: Bundle, label: () -> String) =
        describeTachiyomiPackage(packageName, versionName, metaData, label, this)

    override suspend fun createSources(
        context: Context,
        appInfo: ApplicationInfo,
        packageName: String,
        description: ExtensionDescription,
        keys: LoadPassSourceKeys,
    ): List<UnifiedSource> {
        return createTachiyomiSources(description.sourceClassNames, classLoader(appInfo, context))
    }

    private fun classLoader(appInfo: ApplicationInfo, context: Context): ClassLoader {
        // ART maps app images compiled for the built-in loader's class loader chain, and before Android 17 a damaged
        // one crashes every launch until the app is updated.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            try {
                // Installed APKs are optimized for PathClassLoader. Loading a private cache copy
                // prevents ART from reusing that incompatible context with DelegateLastClassLoader.
                val cachedExtension = cacheExtensionApk(
                    source = File(appInfo.sourceDir),
                    cacheDir = File(context.codeCacheDir, "extension_apks"),
                    packageName = appInfo.packageName,
                )
                return DelegateLastClassLoader(cachedExtension.absolutePath, null, context.classLoader)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) {
                    "Failed to prepare the optimized extension class loader for ${appInfo.packageName}"
                }
            }
        }

        return DelegateLastPathClassLoader(appInfo.sourceDir, null, context.classLoader)
    }
}

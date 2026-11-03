package eu.kanade.tachiyomi.extension.platform

import android.content.Context
import android.os.Build
import dalvik.system.DelegateLastClassLoader
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.kind.cacheExtensionApk
import eu.kanade.tachiyomi.extension.util.DelegateLastPathClassLoader
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/** Class loaders over the installed apk, or on Android 17+ for a kind that prefers ART's own, over a private copy. */
@Inject
class AndroidExtensionClassLoaders(private val context: Context) : ExtensionClassLoaders {

    override fun classLoader(
        extensionPackage: ExtensionPackage,
        parentFirstPackagePrefixes: List<String>,
        prefersPlatformLoader: Boolean,
    ): ClassLoader {
        // The loader only asks for the code of a package it read meta-data from, which only an application has
        val appInfo = checkNotNull((extensionPackage as AndroidExtensionPackage).packageInfo.applicationInfo)

        // ART maps app images compiled for the built-in loader's class loader chain, and before Android 17 a damaged
        // one crashes every launch until the app is updated.
        if (prefersPlatformLoader && Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
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

        return DelegateLastPathClassLoader(
            dexPath = appInfo.sourceDir,
            librarySearchPath = null,
            parent = context.classLoader,
            parentFirstPackagePrefixes = parentFirstPackagePrefixes,
        )
    }
}

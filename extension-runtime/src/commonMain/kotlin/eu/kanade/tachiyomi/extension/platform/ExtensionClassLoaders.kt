package eu.kanade.tachiyomi.extension.platform

import mihon.core.common.platform.PlatformFacility

/** Loads the classes of an apk extension, from its package's code. */
@PlatformFacility
interface ExtensionClassLoaders {

    /**
     * A class loader for [extensionPackage] that looks up a class like Android's `DelegateLastClassLoader`: the
     * platform's own classes first, then the package's, then the app's. Classes under [parentFirstPackagePrefixes] come
     * from the app before the package, so copies an extension bundles can't replace the ones the app shares with it.
     *
     * [prefersPlatformLoader] lets the platform use its own delegate-last loader instead, where it has one that runs
     * code the platform optimized for the package better than a loader of the app's could; only for a kind that names
     * no [parentFirstPackagePrefixes], which such a loader can't honour.
     */
    fun classLoader(
        extensionPackage: ExtensionPackage,
        parentFirstPackagePrefixes: List<String>,
        prefersPlatformLoader: Boolean,
    ): ClassLoader

    /**
     * The class or member of the platform's own that [error], thrown by code these loaders loaded, says is missing,
     * where the platform provides only part of what extensions link against; `null` when that isn't why it was thrown,
     * and always on a platform that provides all of it.
     */
    fun missingPlatformMember(error: Throwable): String? = null
}

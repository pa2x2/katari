package mihon.desktop.extension

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage

/** [NoExtensionPackages] lists no apk, so none is loaded. Stands in until desktop loads apk extensions. */
@Inject
class NoExtensionClassLoaders : ExtensionClassLoaders {
    override fun classLoader(
        extensionPackage: ExtensionPackage,
        parentFirstPackagePrefixes: List<String>,
        prefersPlatformLoader: Boolean,
    ): ClassLoader = error("Desktop loads no apk extensions yet: ${extensionPackage.packageName}")
}

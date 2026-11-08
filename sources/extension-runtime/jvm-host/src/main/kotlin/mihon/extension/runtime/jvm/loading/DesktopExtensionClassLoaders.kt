package mihon.extension.runtime.jvm.loading

import eu.kanade.tachiyomi.extension.platform.ExtensionClassLoaders
import eu.kanade.tachiyomi.extension.platform.ExtensionPackage
import mihon.extension.runtime.jvm.apk.DesktopExtensionPackage
import mihon.extension.runtime.jvm.conversion.ConvertedJarCache
import mihon.extension.runtime.jvm.diagnostics.PlatformGaps

/**
 * Class loaders over the jars apks are converted to, converting an apk first when its jar isn't cached, such as an apk
 * installed before its jar was removed or a converter update. Desktop has no platform loader of its own, so a kind's
 * preference for one changes nothing.
 */
class DesktopExtensionClassLoaders(
    private val jars: ConvertedJarCache,
    private val appLoader: ClassLoader = DesktopExtensionClassLoaders::class.java.classLoader,
) : ExtensionClassLoaders {

    override fun classLoader(
        extensionPackage: ExtensionPackage,
        parentFirstPackagePrefixes: List<String>,
        prefersPlatformLoader: Boolean,
    ): ClassLoader {
        // The loader only asks for the code of a package this platform read
        val apk = extensionPackage as DesktopExtensionPackage
        return ExtensionJarClassLoader(jars.jarOf(apk), appLoader, parentFirstPackagePrefixes)
    }

    override fun missingPlatformMember(error: Throwable): String? = PlatformGaps.missingMember(error)
}

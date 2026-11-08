package mihon.extension.runtime.jvm.loading

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration

/**
 * Loads an extension's converted [jar] the way ART's delegate-last loaders load its apk, so the same copy of each class
 * wins as on Android:
 * 1. the JDK's own classes, as ART's boot class loader's `java.*` come first;
 * 2. the rest of what ART's boot class path holds, the Android classes under [ANDROID_BOOT_PACKAGES], from [appLoader],
 *    where the Android stand-ins are;
 * 3. the classes under [parentFirstPackagePrefixes], from [appLoader], so copies the extension bundles can't replace
 *    the ones the app shares with it;
 * 4. the jar's own classes;
 * 5. the app's.
 *
 * An Android-named class the stand-ins don't have, such as a support library class an extension bundles, comes from
 * the jar, as it does on Android, where the boot class path doesn't have it either.
 */
class ExtensionJarClassLoader(
    jar: Path,
    private val appLoader: ClassLoader,
    private val parentFirstPackagePrefixes: List<String>,
) : URLClassLoader(NAME, arrayOf(jar.toUri().toURL()), appLoader) {

    private val platformLoader: ClassLoader = getPlatformClassLoader()

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name) ?: findInOrder(name)
            if (resolve) resolveClass(loaded)
            return loaded
        }
    }

    private fun findInOrder(name: String): Class<*> {
        try {
            return platformLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {}

        if (ANDROID_BOOT_PACKAGES.any(name::startsWith) || parentFirstPackagePrefixes.any(name::startsWith)) {
            try {
                return appLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        val fromJar = try {
            return findClass(name)
        } catch (e: ClassNotFoundException) {
            e
        }

        return try {
            appLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
            throw fromJar
        }
    }

    override fun getResource(name: String): URL? {
        return platformLoader.getResource(name)
            ?: findResource(name)
            ?: appLoader.getResource(name)
    }

    override fun getResources(name: String): Enumeration<URL> {
        val resources = buildList {
            addAll(platformLoader.getResources(name).toList())
            addAll(findResources(name).toList())
            addAll(appLoader.getResources(name).toList())
        }
        return Collections.enumeration(resources)
    }

    companion object {
        /** Every extension's loader's name, which stack frames of the extensions' classes carry. */
        const val NAME = "katari-extension"

        /** What ART's boot class path holds beyond the JDK's packages. */
        val ANDROID_BOOT_PACKAGES = listOf(
            "android.",
            "dalvik.",
            "org.json.",
            "org.xmlpull.",
            "com.android.internal.",
            "libcore.",
        )
    }
}

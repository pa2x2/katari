package eu.kanade.tachiyomi.extension.util

import dalvik.system.DelegateLastClassLoader
import dalvik.system.PathClassLoader
import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * A [PathClassLoader] that looks up classes like [DelegateLastClassLoader]: the boot class loader first, then its own
 * dex path, then the parent. Classes under [parentFirstPackagePrefixes] come from the parent before the dex path, so
 * copies an extension bundles can't replace the ones the app shares with it.
 *
 * ART only recognizes the exact platform loader classes, so it never maps an app image for this subclass.
 */
internal class DelegateLastPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
    private val parentFirstPackagePrefixes: List<String> = emptyList(),
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    private val bootClassLoader: ClassLoader? = Any::class.java.classLoader

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        if (name != null && parentFirstPackagePrefixes.any(name::startsWith)) {
            return super.loadClass(name, resolve)
        }

        findLoadedClass(name)?.let { return it }

        if (bootClassLoader != null) {
            try {
                return bootClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {}
        }

        val fromDexPath = try {
            return findClass(name)
        } catch (e: ClassNotFoundException) {
            e
        }

        return try {
            parent.loadClass(name)
        } catch (_: ClassNotFoundException) {
            throw fromDexPath
        }
    }

    override fun getResource(name: String?): URL? {
        return bootClassLoader?.getResource(name)
            ?: findResource(name)
            ?: parent?.getResource(name)
    }

    override fun getResources(name: String?): Enumeration<URL> {
        val resources = buildList {
            bootClassLoader?.getResources(name)?.let { addAll(it.toList()) }
            findResources(name)?.let { addAll(it.toList()) }
            parent?.getResources(name)?.let { addAll(it.toList()) }
        }
        return Collections.enumeration(resources)
    }
}

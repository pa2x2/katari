package eu.kanade.tachiyomi.extension.platform

import android.os.Bundle

class AndroidExtensionMetaData(private val bundle: Bundle) : ExtensionMetaData {
    override fun getString(key: String): String? = bundle.getString(key)

    override fun getInt(key: String): Int = bundle.getInt(key)

    override fun getBoolean(key: String): Boolean = bundle.getBoolean(key)

    // Manifests write the extension library as whatever type the build gave it, which only the untyped read returns
    @Suppress("DEPRECATION")
    override fun get(key: String): Any? = bundle.get(key)

    override fun containsKey(key: String): Boolean = bundle.containsKey(key)
}

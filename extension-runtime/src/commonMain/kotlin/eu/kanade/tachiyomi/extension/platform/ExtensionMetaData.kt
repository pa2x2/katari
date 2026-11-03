package eu.kanade.tachiyomi.extension.platform

/**
 * A package's application meta-data, read as Android's `Bundle` reads it: a typed read of a key missing or of another
 * type gives `null`, 0 or `false`, and [get] gives the value as the build wrote it, whatever its type.
 */
interface ExtensionMetaData {
    fun getString(key: String): String?
    fun getInt(key: String): Int
    fun getBoolean(key: String): Boolean
    fun get(key: String): Any?
    fun containsKey(key: String): Boolean

    /** The meta-data of a package that has none. */
    object Empty : ExtensionMetaData {
        override fun getString(key: String): String? = null
        override fun getInt(key: String): Int = 0
        override fun getBoolean(key: String): Boolean = false
        override fun get(key: String): Any? = null
        override fun containsKey(key: String): Boolean = false
    }
}

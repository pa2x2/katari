package mihon.extension.runtime.jvm.apk

import eu.kanade.tachiyomi.extension.platform.ExtensionMetaData
import mihon.extension.runtime.jvm.apk.binary.ResourceValue

/**
 * An apk's application meta-data as Android's package parser puts it in a `Bundle`: an `android:resource` as its
 * resource ID, an `android:value` as a `String`, `Boolean`, `Int` (decimal, hexadecimal and colour values) or `Float`,
 * a reference first resolved through the apk's resources, and any other type left out. A later entry of the same name
 * replaces an earlier one. Typed reads then behave as `Bundle`'s: a missing key or one of another type reads as `null`,
 * 0 or `false`.
 */
internal class ApkMetaData(private val values: Map<String, Any>) : ExtensionMetaData {

    override fun getString(key: String): String? = values[key] as? String

    override fun getInt(key: String): Int = values[key] as? Int ?: 0

    override fun getBoolean(key: String): Boolean = values[key] as? Boolean ?: false

    override fun get(key: String): Any? = values[key]

    override fun containsKey(key: String): Boolean = key in values

    companion object {
        /** [resources] resolves values that reference a resource; it's only read when one does. */
        fun of(entries: List<ApkManifest.MetaDataEntry>, resources: () -> ApkResources?): ApkMetaData {
            val values = LinkedHashMap<String, Any>()
            for (entry in entries) {
                if (entry.resourceId != 0) {
                    values[entry.name] = entry.resourceId
                    continue
                }
                val attribute = entry.value ?: continue
                val value = if (attribute.value.type == ResourceValue.REFERENCE) {
                    resources()?.let { resolved(it, attribute.value.data) }
                } else {
                    typed(attribute.value, attribute.string)
                }
                if (value != null) values[entry.name] = value
            }
            return ApkMetaData(values)
        }

        private fun resolved(resources: ApkResources, resourceId: Int): Any? {
            val value = resources.resolve(resourceId) ?: return null
            return if (value.type == ResourceValue.STRING) resources.string(resourceId) else typed(value, null)
        }

        private fun typed(value: ResourceValue, string: String?): Any? = when {
            value.type == ResourceValue.STRING -> string
            value.type == ResourceValue.INT_BOOLEAN -> value.data != 0
            value.isInt -> value.data
            value.type == ResourceValue.FLOAT -> Float.fromBits(value.data)
            else -> null
        }
    }
}

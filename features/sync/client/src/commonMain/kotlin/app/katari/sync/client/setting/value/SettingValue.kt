package app.katari.sync.client.setting.value

import app.katari.sync.client.payload.SourceReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * A preference's value as stores hold it: one of the typed fields, or none when the preference isn't set and reads as
 * its default. The source keys and the local IDs of other synced records it holds are replaced by indexes into
 * [sources] and [records] ([SettingEncoding]), so every device holds the same value alike.
 */
@Serializable
data class SettingValue(
    @ProtoNumber(1) val string: String? = null,
    @ProtoNumber(2) val int: Int? = null,
    @ProtoNumber(3) val long: Long? = null,
    @ProtoNumber(4) val float: Float? = null,
    @ProtoNumber(5) val boolean: Boolean? = null,
    @ProtoNumber(6) val stringSet: StringSetValue? = null,
    @ProtoNumber(7) val sources: List<SourceReference> = emptyList(),
    /** Record keys, of categories or entries. */
    @ProtoNumber(8) val records: List<String> = emptyList(),
) {
    /** The value as a store holds it, still with the indexes in it. */
    val stored: Any?
        get() = string ?: int ?: long ?: float ?: boolean ?: stringSet?.values?.toSet()

    companion object {
        /** [stored], a value as a store holds it, with [sources] and [records] its indexes stand for. */
        fun of(
            stored: Any?,
            sources: List<SourceReference> = emptyList(),
            records: List<String> = emptyList(),
        ): SettingValue {
            val value = when (stored) {
                null -> SettingValue()
                is String -> SettingValue(string = stored)
                is Int -> SettingValue(int = stored)
                is Long -> SettingValue(long = stored)
                is Float -> SettingValue(float = stored)
                is Boolean -> SettingValue(boolean = stored)
                // A set has no order; sorted, every device writes it alike
                is Set<*> -> SettingValue(stringSet = StringSetValue(stored.map { it as String }.sorted()))
                else -> throw IllegalArgumentException("Preferences can't hold a ${stored::class.simpleName}")
            }
            return value.copy(sources = sources, records = records)
        }
    }
}

/** A message of its own, so an empty set is told apart from no set. */
@Serializable
data class StringSetValue(@ProtoNumber(1) val values: List<String> = emptyList())

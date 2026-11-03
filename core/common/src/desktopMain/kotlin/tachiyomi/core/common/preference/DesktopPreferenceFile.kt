package tachiyomi.core.common.preference

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.core.common.storage.replaceAtomically
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * The JSON file a [DesktopPreferenceStore] keeps its values in. Each value is stored with its type, since a JSON number
 * alone cannot tell an `Int` from a `Long` or a `Float`, and a value has to read back as the type it was written with.
 * Values are `String`, `Long`, `Int`, `Float`, `Boolean` or `Set<String>`, as in Android's shared preferences.
 */
internal class DesktopPreferenceFile(private val path: Path) {

    /** The stored values, or none when the file is missing or unreadable, so a damaged file never stops the app. */
    fun read(): Map<String, Any> {
        val text = try {
            Files.readString(path)
        } catch (_: NoSuchFileException) {
            return emptyMap()
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not read preferences from $path; starting without them" }
            return emptyMap()
        }
        return try {
            json.decodeFromString<Map<String, StoredValue>>(text).mapValues { (_, stored) -> stored.value }
        } catch (e: IllegalArgumentException) {
            logcat(LogPriority.ERROR, e) { "Malformed preferences in $path; starting without them" }
            emptyMap()
        }
    }

    /** Replaces the file with [values], leaving the previous file whole if it fails. Returns whether it succeeded. */
    fun write(values: Map<String, Any>): Boolean {
        val bytes = json.encodeToString(values.mapValues { (_, value) -> value.toStoredValue() }).encodeToByteArray()
        return try {
            replaceAtomically(path, bytes)
            true
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not write preferences to $path" }
            false
        }
    }

    /** Removes the file, returning whether it is gone. */
    fun delete(): Boolean {
        return try {
            Files.deleteIfExists(path)
            true
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not delete preferences at $path" }
            false
        }
    }

    private fun Any.toStoredValue(): StoredValue = when (this) {
        is String -> StoredValue.StringValue(this)
        is Long -> StoredValue.LongValue(this)
        is Int -> StoredValue.IntValue(this)
        is Float -> StoredValue.FloatValue(this)
        is Boolean -> StoredValue.BooleanValue(this)
        is Set<*> -> StoredValue.StringSetValue(mapTo(LinkedHashSet()) { it as String })
        else -> error("Preferences cannot store a ${this::class.simpleName}")
    }

    @Serializable
    private sealed interface StoredValue {
        val value: Any

        @Serializable
        @SerialName("string")
        data class StringValue(override val value: String) : StoredValue

        @Serializable
        @SerialName("long")
        data class LongValue(override val value: Long) : StoredValue

        @Serializable
        @SerialName("int")
        data class IntValue(override val value: Int) : StoredValue

        @Serializable
        @SerialName("float")
        data class FloatValue(override val value: Float) : StoredValue

        @Serializable
        @SerialName("boolean")
        data class BooleanValue(override val value: Boolean) : StoredValue

        @Serializable
        @SerialName("stringSet")
        data class StringSetValue(override val value: Set<String>) : StoredValue
    }

    private companion object {
        val json = Json {
            prettyPrint = true
            allowSpecialFloatingPointValues = true
        }
    }
}

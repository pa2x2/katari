package mihon.entry.interactions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Stable JSON payload codec shared by participants and the finite legacy wire bridge. */
object EntryBackupStateCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun <T> encode(serializer: KSerializer<T>, value: T): ByteArray {
        return json.encodeToString(serializer, value).encodeToByteArray()
    }

    fun <T> decode(serializer: KSerializer<T>, payload: ByteArray): T {
        return json.decodeFromString(serializer, payload.decodeToString())
    }
}

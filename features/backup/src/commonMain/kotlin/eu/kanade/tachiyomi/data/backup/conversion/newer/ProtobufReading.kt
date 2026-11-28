package eu.kanade.tachiyomi.data.backup.conversion.newer

import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.lengthDelimitedTag
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer

/** A protobuf backup's entry, read as [NewerAppReading.entry] says; null when it doesn't decode. */
internal fun <T> NewerAppReading.protobufEntry(
    parser: ProtoBuf,
    shape: MessageShape,
    deserializer: DeserializationStrategy<T>,
    bytes: ByteArray,
): T? = entry(
    title = { protobufTitle(bytes) },
    check = { shape.check(bytes, IN_ENTRY, this) },
    decode = { parser.decodeFromByteArray(deserializer, bytes) },
)

/** A protobuf backup without its entries, read as [NewerAppReading.whole] says. */
internal fun <T> NewerAppReading.protobufWhole(
    parser: ProtoBuf,
    shape: MessageShape,
    deserializer: DeserializationStrategy<T>,
    bytes: ByteArray,
): T = whole(
    check = { shape.check(bytes, IN_BACKUP, this) },
    decode = { parser.decodeFromByteArray(deserializer, bytes) },
)

/** Part of an entry, read as [NewerAppReading.part] says. */
internal fun <T> NewerAppReading.protobufPart(
    parser: ProtoBuf,
    deserializer: DeserializationStrategy<T>,
    bytes: ByteArray,
): T? = part { parser.decodeFromByteArray(deserializer, bytes) }

// The Mihon and Aniyomi families keep an entry's title at 3
private fun protobufTitle(bytes: ByteArray): String? {
    val input = ProtoInput(Buffer().write(bytes))
    while (!input.exhausted()) {
        val tag = input.readTag()
        if (tag == TITLE_TAG) return input.readByteArray(input.readLength()).decodeToString().ifBlank { null }
        input.skipField(tag)
    }
    return null
}

private val TITLE_TAG = lengthDelimitedTag(3)

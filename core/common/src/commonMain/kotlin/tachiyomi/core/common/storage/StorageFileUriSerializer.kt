package tachiyomi.core.common.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Saves a [StorageFile] as its [StorageFile.uri]; reading one back fails where [files] can't open the URI. */
class StorageFileUriSerializer(private val files: StorageFileResolver) : KSerializer<StorageFile> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StorageFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: StorageFile) {
        encoder.encodeString(value.uri)
    }

    override fun deserialize(decoder: Decoder): StorageFile {
        val uri = decoder.decodeString()
        return files.fromUri(uri) ?: throw SerializationException("Unsupported storage URI: $uri")
    }
}

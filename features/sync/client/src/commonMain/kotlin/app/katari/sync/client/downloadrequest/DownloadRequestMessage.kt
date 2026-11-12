package app.katari.sync.client.downloadrequest

import app.katari.sync.protocol.message.MessageBody
import app.katari.sync.protocol.message.MessageLimits
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * What a download request says: download these chapters of the entry whose record has [entryKey]. Chapters are named
 * by URL, as the entry's source lists them on every device, with the name the sender showed, so the device it reaches
 * can say which chapters it couldn't find.
 */
@Serializable
data class DownloadRequestPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val chapters: List<RequestedChapter> = emptyList(),
)

@Serializable
data class RequestedChapter(
    @ProtoNumber(1) val url: String,
    @ProtoNumber(2) val name: String,
)

/** The message kind of download requests, and how its payloads are written. */
object DownloadRequestMessage {
    const val KIND = "download"
    const val VERSION = 1

    fun encode(payload: DownloadRequestPayload): ByteArray = ProtoBuf.encodeToByteArray(
        DownloadRequestPayload.serializer(),
        payload,
    )

    fun decode(bytes: ByteArray): DownloadRequestPayload = ProtoBuf.decodeFromByteArray(
        DownloadRequestPayload.serializer(),
        bytes,
    )

    /** The messages that ask for [chapters] of [entryKey], as many as it takes to keep each within the size limit. */
    fun bodies(entryKey: String, chapters: List<RequestedChapter>): List<MessageBody> {
        val bytes = encode(DownloadRequestPayload(entryKey, chapters))
        if (bytes.size <= MessageLimits.MAX_PAYLOAD_BYTES || chapters.size <= 1) {
            return listOf(MessageBody(KIND, VERSION, bytes))
        }
        val half = chapters.size / 2
        return bodies(entryKey, chapters.subList(0, half)) + bodies(entryKey, chapters.subList(half, chapters.size))
    }
}

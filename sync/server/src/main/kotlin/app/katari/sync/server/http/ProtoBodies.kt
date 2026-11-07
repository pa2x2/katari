package app.katari.sync.server.http

import app.katari.sync.protocol.http.SyncHttp
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondBytes
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

internal val ProtobufContentType: ContentType = ContentType.parse(SyncHttp.CONTENT_TYPE)

internal suspend inline fun <reified T> ApplicationCall.receiveProto(maxBytes: Long): T {
    val bytes = receiveChannel().readBuffer(maxBytes + 1).readByteArray()
    if (bytes.size > maxBytes) throw PayloadTooLargeException(maxBytes)
    return try {
        ProtoBuf.decodeFromByteArray<T>(bytes)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization's SerializationException is one of these.
        throw BadRequestException("Malformed ${T::class.simpleName}", e)
    }
}

internal suspend inline fun <reified T> ApplicationCall.respondProto(
    message: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondBytes(ProtoBuf.encodeToByteArray(message), ProtobufContentType, status)
}

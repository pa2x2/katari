package app.katari.sync.client.server

import app.katari.sync.protocol.account.RedeemInviteRequest
import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkCode
import app.katari.sync.protocol.device.LinkedDevices
import app.katari.sync.protocol.device.PasswordSignIn
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.protocol.device.RevokeDeviceRequest
import app.katari.sync.protocol.error.SyncError
import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.protocol.pull.GroupRecords
import app.katari.sync.protocol.pull.GroupRequest
import app.katari.sync.protocol.pull.PullPage
import app.katari.sync.protocol.pull.PullRequest
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.protocol.push.PushResponse
import app.katari.sync.protocol.server.ServerInfo
import app.katari.sync.protocol.snapshot.SnapshotInfo
import app.katari.sync.protocol.version.SyncProtocolVersion
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.GzipSink
import okio.buffer
import java.io.IOException

/**
 * Every call of the protocol except files, to the server at [server], as the device whose [token] it is (null before
 * the device has one). Bodies are protobuf, request bodies larger than [COMPRESS_FROM] go gzipped, and OkHttp asks
 * for and unpacks gzipped answers itself. Every call but [info] carries the protocol version.
 *
 * Each call returns the server's answer or throws a [SyncFailure].
 */
class SyncServerClient internal constructor(
    private val http: OkHttpClient,
    val server: HttpUrl,
    private val token: String?,
) {
    suspend fun info(): ServerInfo = call(get(SyncHttp.INFO, versioned = false))

    suspend fun signIn(device: DeviceDescription, username: String, password: String): RegisterDeviceResponse =
        call(post(SyncHttp.REGISTER, RegisterDeviceRequest(device, password = PasswordSignIn(username, password))))

    suspend fun redeemLinkCode(device: DeviceDescription, code: String): RegisterDeviceResponse =
        call(post(SyncHttp.REGISTER, RegisterDeviceRequest(device, linkCode = code)))

    suspend fun redeemInvite(
        device: DeviceDescription,
        code: String,
        username: String,
        password: String,
    ): RegisterDeviceResponse = call(
        post(SyncHttp.REDEEM_INVITE, RedeemInviteRequest(device, code, username, password)),
    )

    suspend fun devices(): LinkedDevices = call(get(SyncHttp.DEVICES))

    suspend fun createLinkCode(): LinkCode = call(postEmpty(SyncHttp.LINK_CODE))

    /** False when the account has no such device left to remove. */
    suspend fun revoke(device: DeviceId): Boolean {
        val request = post(SyncHttp.REVOKE, RevokeDeviceRequest(device))
        return execute(request) { response ->
            when {
                response.isSuccessful -> true
                response.code == HTTP_NOT_FOUND -> false
                else -> throw failureOf(response)
            }
        }
    }

    suspend fun push(request: PushRequest): PushResponse = call(post(SyncHttp.PUSH, request))

    suspend fun pull(request: PullRequest): PullPage = call(post(SyncHttp.PULL, request))

    suspend fun group(group: String): GroupRecords = call(post(SyncHttp.GROUP, GroupRequest(group)))

    suspend fun snapshot(): SnapshotInfo = call(get(SyncHttp.SNAPSHOT))

    /** Opens the live channel ([LiveChannel]); its frames and its end go to [listener]. */
    internal fun live(listener: LiveChannel.Listener): LiveChannel {
        // OkHttp turns http and https into ws and wss itself.
        val request = authorized(Request.Builder().url(url(SyncHttp.LIVE)), versioned = true).build()
        return LiveChannel(http, request, listener)
    }

    private inline fun <reified T> post(path: String, message: T): Request {
        val bytes = ProtoBuf.encodeToByteArray(serializer<T>(), message)
        val builder = Request.Builder().url(url(path))
        if (bytes.size < COMPRESS_FROM) {
            builder.post(bytes.toRequestBody(PROTOBUF))
        } else {
            val gzipped = Buffer().also { buffer -> GzipSink(buffer).buffer().use { it.write(bytes) } }
            builder.post(gzipped.readByteArray().toRequestBody(PROTOBUF)).header(CONTENT_ENCODING, "gzip")
        }
        return authorized(builder, versioned = true).build()
    }

    private fun postEmpty(path: String): Request =
        authorized(Request.Builder().url(url(path)).post(ByteArray(0).toRequestBody()), versioned = true).build()

    private fun get(path: String, versioned: Boolean = true): Request =
        authorized(Request.Builder().url(url(path)).get(), versioned).build()

    private fun authorized(builder: Request.Builder, versioned: Boolean): Request.Builder = builder.apply {
        if (versioned) header(SyncHttp.PROTOCOL_VERSION_HEADER, SyncProtocolVersion.CURRENT.toString())
        token?.let { header(AUTHORIZATION, "Bearer $it") }
    }

    // The server's address may carry a path of its own, as behind a reverse proxy that serves it under one.
    private fun url(path: String): HttpUrl = server.newBuilder().addPathSegments(path.removePrefix("/")).build()

    private suspend inline fun <reified T> call(request: Request): T = execute(request) { response ->
        if (!response.isSuccessful) throw failureOf(response)
        decode(serializer<T>(), response) ?: throw SyncFailure.NotASyncServer(response.code)
    }

    private suspend fun <T> execute(request: Request, read: (Response) -> T): T {
        val response = try {
            http.newCall(request).await()
        } catch (e: IOException) {
            throw SyncFailure.Unreachable(e)
        }
        return withContext(Dispatchers.IO) {
            response.use {
                try {
                    read(it)
                } catch (e: IOException) {
                    throw SyncFailure.Unreachable(e)
                }
            }
        }
    }

    private fun failureOf(response: Response): SyncFailure {
        val error = decode(SyncError.serializer(), response) ?: return SyncFailure.NotASyncServer(response.code)
        return SyncFailure.Refused(error, withToken = token != null)
    }

    /** The protobuf body of [response], or null when it isn't one, such as a proxy's error page. */
    private fun <T> decode(serializer: KSerializer<T>, response: Response): T? {
        val type = response.body.contentType()
        if (type == null || "${type.type}/${type.subtype}" != SyncHttp.CONTENT_TYPE) return null
        return try {
            ProtoBuf.decodeFromByteArray(serializer, response.body.bytes())
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val CONTENT_ENCODING = "Content-Encoding"
        const val HTTP_NOT_FOUND = 404

        // Below this, gzip's header and the work cost more than the bytes saved.
        const val COMPRESS_FROM = 1024
        val PROTOBUF = SyncHttp.CONTENT_TYPE.toMediaType()
    }
}

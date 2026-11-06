package app.katari.sync.client.server

import app.katari.sync.protocol.account.RedeemInviteRequest
import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkCode
import app.katari.sync.protocol.device.LinkCodeAccount
import app.katari.sync.protocol.device.LinkCodeAccountRequest
import app.katari.sync.protocol.device.LinkedDevices
import app.katari.sync.protocol.device.PasswordSignIn
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.device.RegisterDeviceResponse
import app.katari.sync.protocol.device.RevokeDeviceRequest
import app.katari.sync.protocol.error.SyncError
import app.katari.sync.protocol.file.FileClaimRequest
import app.katari.sync.protocol.file.FileClaimResponse
import app.katari.sync.protocol.file.FileReference
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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.GzipSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.time.Duration

/**
 * Every call of the protocol to the server at [server], as the device whose [token] it is (null before
 * the device has one). Bodies are protobuf, request bodies larger than [COMPRESS_FROM] go gzipped, and OkHttp asks
 * for and unpacks gzipped answers itself. Every call but [info] carries the protocol version. Files travel as raw
 * bytes, with no limit on how long the whole call takes, since a large font on a slow network takes a while.
 *
 * Each call returns the server's answer or throws a [SyncFailure].
 */
class SyncServerClient internal constructor(
    private val http: OkHttpClient,
    val server: HttpUrl,
    private val token: String?,
) {
    /** Whether the server has answered any call made through this client, whatever it answered. */
    @Volatile
    var answered: Boolean = false
        private set

    suspend fun info(): ServerInfo = call(get(SyncHttp.INFO, versioned = false))

    suspend fun signIn(device: DeviceDescription, username: String, password: String): RegisterDeviceResponse =
        call(post(SyncHttp.REGISTER, RegisterDeviceRequest(device, password = PasswordSignIn(username, password))))

    suspend fun redeemLinkCode(device: DeviceDescription, code: String): RegisterDeviceResponse =
        call(post(SyncHttp.REGISTER, RegisterDeviceRequest(device, linkCode = code)))

    /** The account [code] joins, leaving the code to join with. */
    suspend fun accountOfLinkCode(code: String): LinkCodeAccount =
        call(post(SyncHttp.LINK_CODE_ACCOUNT, LinkCodeAccountRequest(code)))

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

    /** Claims [files] for records about to be pushed and returns those the server lacks. */
    suspend fun claimFiles(files: List<FileReference>): FileClaimResponse =
        call(post(SyncHttp.FILE_CLAIM, FileClaimRequest(files)))

    /**
     * Uploads [source] as [file]. False when the server turned the bytes down, as not hashing to [file]'s name, which
     * happens when the file changed while it was read, or as too large.
     */
    suspend fun uploadFile(file: FileReference, source: File): Boolean {
        val body = source.asRequestBody(OCTET_STREAM)
        val request = authorized(Request.Builder().url(url(SyncHttp.filePath(file.category, file.sha256))).put(body))
            .build()
        return execute(request, fileHttp) { response ->
            when {
                response.isSuccessful -> true
                isSyncError(response) -> throw failureOf(response)
                response.code == HTTP_BAD_REQUEST || response.code == HTTP_TOO_LARGE -> false
                else -> throw SyncFailure.NotASyncServer(response.code)
            }
        }
    }

    /** Downloads [file] into [target]; false when the server has no such file. The bytes are not checked here. */
    suspend fun downloadFile(file: FileReference, target: File): Boolean {
        val request = authorized(Request.Builder().url(url(SyncHttp.filePath(file.category, file.sha256))).get())
            .build()
        return execute(request, fileHttp) { response ->
            when {
                response.isSuccessful -> {
                    target.sink().use { response.body.source().readAll(it) }
                    true
                }
                isSyncError(response) -> throw failureOf(response)
                response.code == HTTP_NOT_FOUND -> false
                else -> throw SyncFailure.NotASyncServer(response.code)
            }
        }
    }

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

    private fun authorized(builder: Request.Builder, versioned: Boolean = true): Request.Builder = builder.apply {
        if (versioned) header(SyncHttp.PROTOCOL_VERSION_HEADER, SyncProtocolVersion.CURRENT.toString())
        token?.let { header(AUTHORIZATION, "Bearer $it") }
    }

    // The server's address may carry a path of its own, as behind a reverse proxy that serves it under one.
    private fun url(path: String): HttpUrl = server.newBuilder().addPathSegments(path.removePrefix("/")).build()

    private suspend inline fun <reified T> call(request: Request): T = execute(request) { response ->
        if (!response.isSuccessful) throw failureOf(response)
        decode(serializer<T>(), response) ?: throw SyncFailure.NotASyncServer(response.code)
    }

    private suspend fun <T> execute(request: Request, client: OkHttpClient = http, read: (Response) -> T): T {
        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            throw SyncFailure.Unreachable(e)
        }
        answered = true
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

    private val fileHttp: OkHttpClient by lazy { http.newBuilder().callTimeout(Duration.ZERO).build() }

    private fun isSyncError(response: Response): Boolean = response.body.contentType()
        ?.let { "${it.type}/${it.subtype}" == SyncHttp.CONTENT_TYPE } == true

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
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_LARGE = 413

        // Below this, gzip's header and the work cost more than the bytes saved.
        const val COMPRESS_FROM = 1024
        val PROTOBUF = SyncHttp.CONTENT_TYPE.toMediaType()
        val OCTET_STREAM = "application/octet-stream".toMediaType()
    }
}

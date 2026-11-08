package app.katari.sync.server.http

import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileClaimRequest
import app.katari.sync.protocol.http.SyncHttp
import app.katari.sync.server.SyncServices
import app.katari.sync.server.file.UploadOutcome
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.LocalPathContent
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Route.fileRoutes(services: SyncServices) {
    post(SyncHttp.FILE_CLAIM) {
        val request = call.receiveProto<FileClaimRequest>(services.config.maxRequestBytes)
        request.files.forEach(RequestChecks::check)
        call.respondProto(withContext(Dispatchers.IO) { services.files.claim(call.device.accountId, request) })
    }
    put("${SyncHttp.FILES}/{category}/{sha256}") {
        val (category, sha256) = call.fileName()
        val bytes = call.receiveChannel().toInputStream()
        val outcome = withContext(Dispatchers.IO) {
            services.files.upload(call.device.accountId, category, sha256, bytes)
        }
        when (outcome) {
            UploadOutcome.Stored -> call.respond(HttpStatusCode.NoContent)
            UploadOutcome.HashMismatch -> throw BadRequestException("The file does not hash to $sha256")
            UploadOutcome.TooLarge -> throw PayloadTooLargeException(services.config.maxFileBytes)
        }
    }
    get("${SyncHttp.FILES}/{category}/{sha256}") {
        val (category, sha256) = call.fileName()
        val path = withContext(Dispatchers.IO) { services.files.download(call.device.accountId, category, sha256) }
            ?: throw NotFoundException()
        call.respond(LocalPathContent(path, ContentType.Application.OctetStream))
    }
}

private fun RoutingCall.fileName(): Pair<FileCategory, String> {
    val category = parameters["category"]?.let(SyncHttp::fileCategoryOf) ?: throw NotFoundException()
    val sha256 = parameters["sha256"]?.takeIf(RequestChecks::isSha256) ?: throw NotFoundException()
    return category to sha256
}

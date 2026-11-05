package app.katari.sync.server.http

import app.katari.sync.protocol.device.DeviceDescription
import app.katari.sync.protocol.device.RegisterDeviceRequest
import app.katari.sync.protocol.file.FileReference
import app.katari.sync.protocol.pull.PullRequest
import app.katari.sync.protocol.push.PushRequest
import app.katari.sync.protocol.record.RecordEnvelope
import io.ktor.server.plugins.BadRequestException

/**
 * The shape rules of requests, checked before they reach the services, which take them as well formed. Limits keep
 * one device from storing unbounded keys or names; payload sizes are bounded by the request size instead.
 */
internal object RequestChecks {
    private const val MAX_KEY_LENGTH = 512
    private const val MAX_KIND_LENGTH = 128
    private const val MAX_CHANGE_ID_LENGTH = 128
    private const val MAX_DESCRIPTION_LENGTH = 128
    private val sha256Hex = Regex("[0-9a-f]{64}")

    fun check(request: RegisterDeviceRequest) {
        require((request.password == null) != (request.linkCode == null)) {
            "Exactly one of a password and a link code"
        }
        check(request.device)
    }

    fun check(request: PushRequest) {
        request.records.forEach(::check)
    }

    fun check(request: PullRequest) {
        require(request.after >= 0 && request.limit >= 0) { "Negative position or limit" }
    }

    fun isSha256(text: String): Boolean = sha256Hex.matches(text)

    fun check(file: FileReference) {
        require(isSha256(file.sha256) && file.size >= 0) { "Malformed file reference" }
    }

    private fun check(record: RecordEnvelope) {
        require(record.key.length in 1..MAX_KEY_LENGTH) { "Record key length" }
        require(record.kind.length in 1..MAX_KIND_LENGTH) { "Record kind length" }
        require(record.changeId.length in 1..MAX_CHANGE_ID_LENGTH) { "Change id length" }
        require(record.basedOn >= 0) { "Negative based-on sequence" }
        record.files.forEach(::check)
    }

    private fun check(description: DeviceDescription) {
        listOf(description.name, description.platform, description.appVersion).forEach {
            require(it.isNotBlank() && it.length <= MAX_DESCRIPTION_LENGTH) { "Device description field length" }
        }
    }

    private inline fun require(condition: Boolean, message: () -> String) {
        if (!condition) throw BadRequestException(message())
    }
}

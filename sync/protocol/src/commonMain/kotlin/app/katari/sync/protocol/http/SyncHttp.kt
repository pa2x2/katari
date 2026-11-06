package app.katari.sync.protocol.http

import app.katari.sync.protocol.file.FileCategory

/**
 * How the protocol's messages travel over HTTP. Request and answer bodies are single protobuf messages of this
 * protocol, which either side may compress with `Content-Encoding: gzip`.
 *
 * Every call except [INFO] carries [PROTOCOL_VERSION_HEADER], and every call except [INFO], [REGISTER] and
 * [REDEEM_INVITE] carries `Authorization: Bearer <token>` with the token registration gave the device. A refused call answers with a
 * non-2xx status and a [app.katari.sync.protocol.error.SyncError] body, except for malformed requests, which get a
 * plain 4xx.
 */
object SyncHttp {
    /** The app's [app.katari.sync.protocol.version.SyncProtocolVersion.CURRENT], as a decimal integer. */
    const val PROTOCOL_VERSION_HEADER: String = "Katari-Sync-Protocol"

    const val CONTENT_TYPE: String = "application/x-protobuf"

    /** GET: [app.katari.sync.protocol.server.ServerInfo]. Needs neither the version header nor a token. */
    const val INFO: String = "/api/info"

    /**
     * POST [app.katari.sync.protocol.device.RegisterDeviceRequest]:
     * [app.katari.sync.protocol.device.RegisterDeviceResponse].
     */
    const val REGISTER: String = "/api/devices/register"

    /**
     * POST [app.katari.sync.protocol.account.RedeemInviteRequest]:
     * [app.katari.sync.protocol.device.RegisterDeviceResponse].
     */
    const val REDEEM_INVITE: String = "/api/invites/redeem"

    /** GET: [app.katari.sync.protocol.device.LinkedDevices]. */
    const val DEVICES: String = "/api/devices"

    /** POST, no body: a [app.katari.sync.protocol.device.LinkCode] for this device's account. */
    const val LINK_CODE: String = "/api/devices/link-code"

    /** POST [app.katari.sync.protocol.device.RevokeDeviceRequest]: no body. */
    const val REVOKE: String = "/api/devices/revoke"

    /** POST [app.katari.sync.protocol.push.PushRequest]: [app.katari.sync.protocol.push.PushResponse]. */
    const val PUSH: String = "/api/records/push"

    /** POST [app.katari.sync.protocol.pull.PullRequest]: [app.katari.sync.protocol.pull.PullPage]. */
    const val PULL: String = "/api/records/pull"

    /** GET: [app.katari.sync.protocol.snapshot.SnapshotInfo]. */
    const val SNAPSHOT: String = "/api/snapshot"

    /** POST [app.katari.sync.protocol.file.FileClaimRequest]: [app.katari.sync.protocol.file.FileClaimResponse]. */
    const val FILE_CLAIM: String = "/api/files/claim"

    /** The parent of [filePath]. */
    const val FILES: String = "/api/files"

    /**
     * WebSocket. Each binary frame is one [app.katari.sync.protocol.live.ChangesAvailable]; the first is sent on
     * connecting, so a device also learns of changes made while it was not listening.
     */
    const val LIVE: String = "/api/live"

    /**
     * PUT the file's bytes to upload it, which the server checks against [sha256]; GET downloads them. Both are raw
     * bytes, not protobuf.
     */
    fun filePath(category: FileCategory, sha256: String): String = "$FILES/${category.pathSegment}/$sha256"

    fun fileCategoryOf(pathSegment: String): FileCategory? = FileCategory.entries.firstOrNull {
        it.pathSegment == pathSegment
    }

    private val FileCategory.pathSegment: String
        get() = when (this) {
            FileCategory.COVER -> "covers"
            FileCategory.FONT -> "fonts"
        }
}

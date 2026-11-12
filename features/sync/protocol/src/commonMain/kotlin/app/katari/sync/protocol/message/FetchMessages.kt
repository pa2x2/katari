package app.katari.sync.protocol.message

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Asks for the messages waiting for this device after [after], oldest first. Taking [after] as handled, the server
 * drops every message up to it, so this is also how a device confirms the messages it was handed.
 */
@Serializable
data class FetchMessagesRequest(
    @ProtoNumber(1) val after: Long = 0,
    /**
     * With nothing waiting, how long the server holds the answer back for a message to come, at most
     * [MAX_WAIT_MILLIS]; 0 answers at once. A device whose live channel doesn't get through waits this way instead.
     */
    @ProtoNumber(2) val waitMillis: Long = 0,
) {
    companion object {
        /** Under the idle timeouts of common reverse proxies. */
        const val MAX_WAIT_MILLIS: Long = 50_000
    }
}

@Serializable
data class MessagePage(
    @ProtoNumber(1) val messages: List<DeviceMessage> = emptyList(),
    /** More are waiting past the last of [messages]; fetching after it gets them. */
    @ProtoNumber(2) val hasMore: Boolean = false,
)

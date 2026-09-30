package mihon.translation.provider.server.call

/** A call to a translation server gave no usable answer. It never carries what was sent or what was answered. */
class ServerCallException(
    val failure: ServerCallFailure,
    cause: Throwable? = null,
) : Exception(
    when (failure) {
        ServerCallFailure.Connection -> "Translation server connection failed"
        is ServerCallFailure.Status -> "Translation server answered with status ${failure.code}"
        ServerCallFailure.InvalidAnswer -> "Translation server returned an invalid answer"
    },
    cause,
) {
    /** The HTTP status the server answered with, when it answered at all. */
    val status: Int?
        get() = (failure as? ServerCallFailure.Status)?.code

    /** Whether the server refused the request itself, as it does one it cannot read or does not allow. */
    val isRejection: Boolean
        get() = status in 400..499
}

sealed interface ServerCallFailure {
    /** The server was not reached, or the connection ended before its answer was read. */
    data object Connection : ServerCallFailure

    /** The server answered with the HTTP status [code] instead of what it was asked for. */
    data class Status(val code: Int) : ServerCallFailure

    /** The server's answer is not what its API describes. */
    data object InvalidAnswer : ServerCallFailure
}

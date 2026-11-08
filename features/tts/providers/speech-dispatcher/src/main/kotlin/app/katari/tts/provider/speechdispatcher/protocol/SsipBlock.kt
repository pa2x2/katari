package app.katari.tts.provider.speechdispatcher.protocol

/**
 * What Speech Dispatcher sends as one block of lines: `NNN-data` lines, then a closing `NNN text` line. Event blocks
 * (7xx) arrive between command replies whenever a message's speech changes, so the reader tells them apart by code.
 */
internal sealed interface SsipBlock

internal data class SsipReply(
    val code: Int,
    val data: List<String>,
    val text: String,
) : SsipBlock {
    val isSuccess: Boolean
        get() = code in 200..299

    /** The closing line as Speech Dispatcher wrote it, such as `409 ERR RATE TOO HIGH`. */
    val statusLine: String
        get() = "$code $text"
}

internal data class SsipEvent(
    val messageId: Long,
    val kind: SsipEventKind,
) : SsipBlock

internal enum class SsipEventKind {
    IndexMark,
    Begin,
    End,
    Canceled,
    Paused,
    Resumed,
}

/** Gathers lines into blocks; returns a block once its closing line arrives, otherwise null. */
internal class SsipBlockAssembler {
    private val data = mutableListOf<String>()

    fun accept(line: String): SsipBlock? {
        val code = line.take(CODE_LENGTH).toIntOrNull()
            ?: throw SsipProtocolException("Speech Dispatcher sent a line without a status code: '$line'")
        val separator = line.getOrNull(CODE_LENGTH)
        val content = line.drop(CODE_LENGTH + 1)
        return when (separator) {
            '-' -> {
                data += content
                null
            }
            ' ', null -> {
                val lines = data.toList()
                data.clear()
                if (code / 100 == EVENT_CLASS) event(code, lines) else SsipReply(code, lines, content)
            }
            else -> throw SsipProtocolException("Speech Dispatcher sent a malformed line: '$line'")
        }
    }

    private fun event(code: Int, lines: List<String>): SsipEvent {
        val messageId = lines.firstOrNull()?.toLongOrNull()
            ?: throw SsipProtocolException("Speech Dispatcher sent event $code without a message id")
        val kind = when (code) {
            700 -> SsipEventKind.IndexMark
            701 -> SsipEventKind.Begin
            702 -> SsipEventKind.End
            703 -> SsipEventKind.Canceled
            704 -> SsipEventKind.Paused
            705 -> SsipEventKind.Resumed
            else -> throw SsipProtocolException("Speech Dispatcher sent unknown event $code")
        }
        return SsipEvent(messageId, kind)
    }

    private companion object {
        const val CODE_LENGTH = 3
        const val EVENT_CLASS = 7
    }
}

internal class SsipProtocolException(message: String) : RuntimeException(message)

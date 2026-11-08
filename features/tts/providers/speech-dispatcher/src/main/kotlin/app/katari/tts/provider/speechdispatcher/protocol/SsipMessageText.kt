package app.katari.tts.provider.speechdispatcher.protocol

/**
 * The body of a SPEAK command. A line holding a single dot ends the message, so every line that starts with a dot gets
 * a second one, which Speech Dispatcher removes, and line breaks become the protocol's CR LF.
 */
internal fun ssipMessageBody(text: String): String = buildString {
    text.lineSequence().forEach { line ->
        if (line.startsWith('.')) append('.')
        append(line)
        append(SSIP_LINE_END)
    }
    append('.')
    append(SSIP_LINE_END)
}

internal const val SSIP_LINE_END = "\r\n"

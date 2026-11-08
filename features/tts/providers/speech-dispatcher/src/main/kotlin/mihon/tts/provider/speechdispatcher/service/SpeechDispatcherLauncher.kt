package mihon.tts.provider.speechdispatcher.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Starts Speech Dispatcher on its socket as its client library does. The started server quits a few seconds after its
 * last client leaves, so a connection is opened right after a start.
 */
internal class SpeechDispatcherLauncher(
    private val environment: Map<String, String>,
) {
    fun isInstalled(): Boolean = executable() != null

    suspend fun start(socket: Path): SpeechDispatcherStart = withContext(Dispatchers.IO) {
        val executable = executable() ?: return@withContext SpeechDispatcherStart.NotInstalled
        val process = ProcessBuilder(
            executable.toString(),
            "--spawn",
            "--communication-method",
            "unix_socket",
            "--socket-path",
            socket.toString(),
        )
            .redirectErrorStream(true)
            .start()
        // The started server detaches from the output, so it ends when the starting process does.
        val output = process.inputStream.bufferedReader().use { it.readLines() }
        if (!process.waitFor(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return@withContext SpeechDispatcherStart.Failed(detail = null)
        }
        if (process.exitValue() == 0) {
            SpeechDispatcherStart.Started
        } else {
            SpeechDispatcherStart.Failed(startFailureDetail(output))
        }
    }

    private fun executable(): Path? {
        return environment["PATH"].orEmpty()
            .split(':')
            .filter(String::isNotBlank)
            .map { Path.of(it, EXECUTABLE) }
            .firstOrNull { Files.isRegularFile(it) && Files.isExecutable(it) }
    }

    private companion object {
        const val EXECUTABLE = "speech-dispatcher"
        const val START_TIMEOUT_SECONDS = 10L
    }
}

internal sealed interface SpeechDispatcherStart {
    data object Started : SpeechDispatcherStart

    data object NotInstalled : SpeechDispatcherStart

    /** [detail] is what Speech Dispatcher printed about the failure; null when it printed nothing or never finished. */
    data class Failed(val detail: String?) : SpeechDispatcherStart
}

/** The server's own explanation, without the "starting" line it prints before every attempt. */
internal fun startFailureDetail(output: List<String>): String? {
    return output
        .map(String::trim)
        .filter { it.isNotEmpty() && !STARTING_LINE.containsMatchIn(it) }
        .joinToString(" ")
        .ifEmpty { null }
}

private val STARTING_LINE = Regex("""speechd: Speech Dispatcher \S+ starting$""")

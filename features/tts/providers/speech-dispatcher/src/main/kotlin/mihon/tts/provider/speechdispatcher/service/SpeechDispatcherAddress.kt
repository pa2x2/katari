package mihon.tts.provider.speechdispatcher.service

import java.nio.file.Path

/** Where Speech Dispatcher listens, found the way its client library finds it. */
internal sealed interface SpeechDispatcherAddress {
    data class UnixSocket(val path: Path) : SpeechDispatcherAddress

    data class InetSocket(val host: String, val port: Int) : SpeechDispatcherAddress
}

/**
 * `SPEECHD_ADDRESS` (`unix_socket[:path]` or `inet_socket[:host[:port]]`) when set, otherwise the socket in the user's
 * runtime directory, which falls back to the cache directory as GLib's does when the session sets none.
 */
internal fun speechDispatcherAddress(
    environment: Map<String, String>,
    home: String,
): SpeechDispatcherAddress {
    val defaultSocket = SpeechDispatcherAddress.UnixSocket(defaultSocketPath(environment, home))
    val configured = environment["SPEECHD_ADDRESS"]?.takeIf(String::isNotBlank) ?: return defaultSocket
    val parts = configured.split(':', limit = 3)
    return when (parts.first()) {
        "unix_socket" -> parts.getOrNull(1)
            ?.takeIf(String::isNotBlank)
            ?.let { SpeechDispatcherAddress.UnixSocket(Path.of(configured.substringAfter(':'))) }
            ?: defaultSocket
        "inet_socket" -> SpeechDispatcherAddress.InetSocket(
            host = parts.getOrNull(1)?.takeIf(String::isNotBlank) ?: DEFAULT_HOST,
            port = parts.getOrNull(2)?.toIntOrNull() ?: DEFAULT_PORT,
        )
        else -> defaultSocket
    }
}

private fun defaultSocketPath(environment: Map<String, String>, home: String): Path {
    val runtimeDirectory = environment["XDG_RUNTIME_DIR"]?.takeIf(String::isNotBlank)
        ?: environment["XDG_CACHE_HOME"]?.takeIf(String::isNotBlank)
        ?: Path.of(home, ".cache").toString()
    return Path.of(runtimeDirectory, "speech-dispatcher", "speechd.sock")
}

private const val DEFAULT_HOST = "127.0.0.1"
private const val DEFAULT_PORT = 6560

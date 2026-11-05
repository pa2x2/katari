package app.katari.sync.server.config

import java.nio.file.Path
import kotlin.io.path.Path

/** What the person running the server sets, from environment variables so a container is configured the usual way. */
data class ServerConfig(
    val dataDirectory: Path,
    val host: String,
    val port: Int,
    /** The largest protobuf body the server reads, after decompression. */
    val maxRequestBytes: Long,
    val maxFileBytes: Long,
    /** The most records one pull page holds; a device asking for more, or for 0, gets this many. */
    val pullPageSize: Int,
) {
    val databaseFile: Path
        get() = dataDirectory.resolve("katari-sync.db")

    companion object {
        const val DATA_DIRECTORY = "KATARI_SYNC_DATA_DIR"
        const val HOST = "KATARI_SYNC_HOST"
        const val PORT = "KATARI_SYNC_PORT"
        const val MAX_REQUEST_BYTES = "KATARI_SYNC_MAX_REQUEST_BYTES"
        const val MAX_FILE_BYTES = "KATARI_SYNC_MAX_FILE_BYTES"
        const val PULL_PAGE_SIZE = "KATARI_SYNC_PULL_PAGE_SIZE"

        private const val MEBIBYTE = 1024L * 1024

        /** Fails with a message naming the variable when one is set to something unusable. */
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ServerConfig {
            fun value(name: String): String? = environment[name]?.trim()?.takeIf(String::isNotEmpty)

            fun number(name: String, default: Long, range: LongRange): Long {
                val text = value(name) ?: return default
                val parsed = text.toLongOrNull()
                require(parsed != null && parsed in range) { "$name must be a whole number in $range, not \"$text\"" }
                return parsed
            }

            return ServerConfig(
                dataDirectory = Path(value(DATA_DIRECTORY) ?: "/data"),
                host = value(HOST) ?: "0.0.0.0",
                port = number(PORT, default = 8080, range = 1L..65535).toInt(),
                maxRequestBytes = number(MAX_REQUEST_BYTES, default = 32 * MEBIBYTE, range = MEBIBYTE..Int.MAX_VALUE),
                // Covers are well under this; CJK fonts reach tens of megabytes.
                maxFileBytes = number(MAX_FILE_BYTES, default = 64 * MEBIBYTE, range = 1L..Long.MAX_VALUE),
                pullPageSize = number(PULL_PAGE_SIZE, default = 500, range = 1L..10_000).toInt(),
            )
        }
    }
}

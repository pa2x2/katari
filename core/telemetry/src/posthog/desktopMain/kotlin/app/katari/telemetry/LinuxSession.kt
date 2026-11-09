package app.katari.telemetry

import java.io.File
import java.io.IOException

/**
 * The Linux system the desktop app runs on, which tells most display and desktop integration problems apart: the
 * distribution from `os-release`, and the desktop and session type its session sets. A Wayland session runs the app
 * through XWayland. Each is null where the system doesn't say.
 */
internal data class LinuxSession(
    val distribution: String?,
    val distributionVersion: String?,
    val desktop: String?,
    val sessionType: String?,
) {
    val properties: Map<String, String>
        get() = buildMap {
            distribution?.let { put("linux_distribution", it) }
            distributionVersion?.let { put("linux_distribution_version", it) }
            desktop?.let { put("desktop_environment", it) }
            sessionType?.let { put("session_type", it) }
        }

    companion object {
        fun read(): LinuxSession {
            val osRelease = OS_RELEASE_FILES.firstNotNullOfOrNull(::readOsRelease).orEmpty()
            return LinuxSession(
                distribution = osRelease["ID"],
                distributionVersion = osRelease["VERSION_ID"],
                desktop = System.getenv("XDG_CURRENT_DESKTOP")?.takeIf { it.isNotEmpty() },
                sessionType = System.getenv("XDG_SESSION_TYPE")?.takeIf { it.isNotEmpty() },
            )
        }

        // Lines of KEY=value, the value maybe quoted; https://www.freedesktop.org/software/systemd/man/os-release.html
        private fun readOsRelease(file: File): Map<String, String>? {
            val lines = try {
                file.readLines()
            } catch (e: IOException) {
                return null
            }
            return lines
                .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 } }
                .associate { (key, value) -> key.trim() to value.trim().removeSurrounding("\"").removeSurrounding("'") }
        }

        // The second is where the system keeps it when /etc has none
        private val OS_RELEASE_FILES = listOf(File("/etc/os-release"), File("/usr/lib/os-release"))
    }
}

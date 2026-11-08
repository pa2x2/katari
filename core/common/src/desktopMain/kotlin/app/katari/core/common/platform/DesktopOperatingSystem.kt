package app.katari.core.common.platform

/**
 * The operating systems Katari on desktop runs on. Everything that differs between them sits behind an interface with
 * one implementation per entry, chosen from [current].
 */
enum class DesktopOperatingSystem(
    /** The name user agents give it. */
    val displayName: String,
) {
    LINUX("Linux"),
    ;

    companion object {
        fun current(): DesktopOperatingSystem {
            val osName = System.getProperty("os.name").orEmpty()
            return when {
                osName.startsWith("Linux") -> LINUX
                else -> error("Katari on desktop runs on Linux only, not on $osName")
            }
        }
    }
}

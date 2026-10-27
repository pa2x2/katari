package mihon.desktop.storage

/** The [DesktopDirectories] of the operating system the app runs on. */
fun systemDesktopDirectories(): DesktopDirectories {
    val osName = System.getProperty("os.name")
    return when {
        osName.startsWith("Linux") -> XdgDesktopDirectories()
        else -> error("Katari on desktop runs on Linux only; it has no file locations for $osName")
    }
}

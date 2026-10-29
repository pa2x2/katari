package mihon.desktop.storage

import java.nio.file.Path

/**
 * Where the desktop app keeps its files, as the operating system it runs on lays them out. Each directory exists by
 * the time its getter returns.
 */
interface DesktopDirectories {
    /** The user's own data, such as the database, which the app cannot rebuild. */
    val data: Path

    val configuration: Path

    /** Files the app can rebuild, which the system or the user may delete at any time. */
    val cache: Path

    /** The user's home directory, which holds the folders the user sees and picks from, not the app's own. */
    val home: Path
}

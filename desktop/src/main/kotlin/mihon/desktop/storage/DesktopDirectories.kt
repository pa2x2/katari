package mihon.desktop.storage

import java.nio.file.Path

/**
 * Where the desktop app keeps its files, as the operating system it runs on lays them out. Each of the app's own
 * directories exists by the time its getter returns.
 */
interface DesktopDirectories {
    /** The user's own data, such as the database, which the app cannot rebuild. */
    val data: Path

    val configuration: Path

    /** Files the app can rebuild, which the system or the user may delete at any time. */
    val cache: Path

    /** What the app keeps between runs that is neither the user's data nor rebuildable, such as its logs. */
    val state: Path

    /**
     * What lasts only while the user is logged in and must be reachable by the user's other launches of the app, such
     * as the lock and socket that keep the app to one instance. Where the system gives no such directory, it is
     * [state].
     */
    val runtime: Path

    /** The user's home directory, which holds the folders the user sees and picks from, not the app's own. */
    val home: Path

    /**
     * The user's pictures folder, where saved images go, as on Android; null where the user has none set apart from
     * [home]. It may not exist yet.
     */
    val pictures: Path?
}

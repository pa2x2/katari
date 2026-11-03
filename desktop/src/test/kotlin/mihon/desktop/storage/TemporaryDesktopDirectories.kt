package mihon.desktop.storage

import java.nio.file.Path
import kotlin.io.path.exists

/** A test's temporary directory as every directory of the desktop app. */
internal class TemporaryDesktopDirectories(private val directory: Path) : DesktopDirectories {
    override val data: Path = directory
    override val configuration: Path = directory
    override val cache: Path = directory
    override val state: Path = directory
    override val runtime: Path = directory
    override val home: Path = directory
    override val pictures: Path = directory

    /**
     * Waits until the files at [paths], relative to the directory, are written. Preference stores write in the
     * background, and the temporary directory can only be deleted once they have.
     */
    fun awaitWritten(vararg paths: String) {
        val files = paths.map(directory::resolve)
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!files.all(Path::exists)) {
            check(System.nanoTime() < deadline) { "Not written: ${files.filterNot(Path::exists)}" }
            Thread.sleep(10)
        }
    }
}

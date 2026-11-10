package app.katari.webview.chromium

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.walk

/**
 * The folders under [root], the app's folder for Chromium: one per installed version, named after it, which exists
 * only whole; [downloads], where archives are downloaded to; and a version's folder while it is unpacked, which a
 * finished unpacking renames into place and an unfinished one leaves to be removed.
 */
@OptIn(ExperimentalPathApi::class)
internal class ChromiumFolders(private val root: Path, version: String) {
    val installed: Path = root.resolve(version)
    val downloads: Path = root.resolve(DOWNLOADS)
    private val unpacking: Path = root.resolve("$version$UNPACKING_SUFFIX")

    val isInstalled: Boolean
        get() = installed.resolve(SERVER).isRegularFile()

    val usableBytes: Long
        get() = Files.getFileStore(Files.createDirectories(root)).usableSpace

    /** Whether a program in [root] can run: a disk mounted `noexec` lets no file be one. */
    fun runPrograms(): Boolean {
        val probe = Files.createTempFile(Files.createDirectories(root), "run", null, RUNNABLE)
        try {
            return Files.isExecutable(probe)
        } finally {
            Files.deleteIfExists(probe)
        }
    }

    /**
     * Puts the files [unpack] writes into the folder it is given in place as the installed version, all of them or,
     * where it throws, none.
     */
    fun install(unpack: (Path) -> Unit) {
        unpacking.deleteRecursively()
        try {
            unpack(unpacking)
            if (!Files.isExecutable(unpacking.resolve(SERVER))) throw IOException("The archive has no Chromium to run")
            installed.deleteRecursively()
            Files.move(unpacking, installed, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            unpacking.deleteRecursively()
        }
    }

    fun deleteInstalled() {
        installed.deleteRecursively()
        unpacking.deleteRecursively()
    }

    /** Removes what other versions left; whether one of them was installed. */
    fun deleteOtherVersions(): Boolean {
        if (!root.exists()) return false
        val others = root.listDirectoryEntries().filter { it.isDirectory() && it != installed && it != downloads }
        val hadInstalled = others.any { it.resolve(SERVER).isRegularFile() }
        others.forEach { it.deleteRecursively() }
        return hadInstalled
    }

    fun storedBytes(): Long =
        if (root.exists()) root.walk().filter { it.isRegularFile() }.sumOf { it.fileSize() } else 0

    private companion object {
        const val SERVER = "cef_server"
        const val DOWNLOADS = "downloads"
        const val UNPACKING_SUFFIX = ".unpacking"
        val RUNNABLE = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))
    }
}

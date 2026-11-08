package xyz.nulldev.androidcompat.io

import mihon.extension.compat.AndroidCompatEnvironment
import java.io.File

/**
 * Android file constants, laid out under the folders Katari's desktop host gives the emulated app the way Android lays
 * them out under an app's data and cache folders.
 */
class AndroidFiles(
    environment: AndroidCompatEnvironment,
) {
    private val dataRoot = environment.dataDir
    private val cacheRoot = environment.cacheDir

    val dataDir: File get() = registerFile(dataRoot)
    val filesDir: File get() = registerFile(File(dataRoot, "files"))
    val noBackupFilesDir: File get() = registerFile(File(dataRoot, "no_backup"))
    val externalFilesDirs: List<File> get() = listOf(registerFile(File(externalStorageDir, "files")))
    val obbDirs: List<File> get() = listOf(registerFile(File(externalStorageDir, "obb")))
    val cacheDir: File get() = registerFile(cacheRoot)
    val codeCacheDir: File get() = registerFile(File(dataRoot, "code_cache"))
    val externalCacheDirs: List<File> get() = listOf(registerFile(File(cacheRoot, "external")))
    val externalMediaDirs: List<File> get() = listOf(registerFile(File(externalStorageDir, "media")))
    val rootDir: File get() = registerFile(dataRoot)
    val externalStorageDir: File get() = registerFile(File(dataRoot, "external"))
    val downloadCacheDir: File get() = registerFile(File(cacheRoot, "download"))
    val databasesDir: File get() = registerFile(File(dataRoot, "databases"))

    val prefsDir: File get() = registerFile(File(dataRoot, "shared_prefs"))

    fun registerFile(file: File): File =
        file.apply {
            mkdirs()
        }
}

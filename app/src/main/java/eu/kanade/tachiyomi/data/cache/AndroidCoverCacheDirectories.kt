package eu.kanade.tachiyomi.data.cache

import android.content.Context
import java.io.File

/** Both kinds of cover in the app's external files, or in its internal files where there is no external storage. */
class AndroidCoverCacheDirectories(private val context: Context) : CoverCacheDirectories {
    override val covers: File
        get() = filesDir(COVERS_DIR)

    override val customCovers: File
        get() = filesDir(CUSTOM_COVERS_DIR)

    private fun filesDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }

    private companion object {
        const val COVERS_DIR = "covers"
        const val CUSTOM_COVERS_DIR = "covers/custom"
    }
}

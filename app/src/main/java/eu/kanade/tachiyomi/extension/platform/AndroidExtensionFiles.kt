package eu.kanade.tachiyomi.extension.platform

import android.content.Context
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
class AndroidExtensionFiles(private val context: Context) : ExtensionFiles {
    override val fileExtensionRoot: File
        get() = File(context.filesDir, "extensions")

    override val privateApkDir: File
        get() = File(context.filesDir, "exts")

    override val downloadDir: File
        get() = context.cacheDir
}

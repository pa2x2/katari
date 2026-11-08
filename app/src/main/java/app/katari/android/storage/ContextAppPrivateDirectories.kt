package app.katari.android.storage

import android.content.Context
import app.katari.core.platform.storage.AppPrivateDirectories
import dev.zacsweers.metro.Inject
import java.io.File

@Inject
class ContextAppPrivateDirectories(private val context: Context) : AppPrivateDirectories {
    override val cache: File
        get() = context.cacheDir

    override val files: File
        get() = context.filesDir

    override val noBackupFiles: File
        get() = context.noBackupFilesDir
}

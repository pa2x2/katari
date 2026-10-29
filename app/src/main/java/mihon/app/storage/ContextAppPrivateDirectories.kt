package mihon.app.storage

import android.content.Context
import dev.zacsweers.metro.Inject
import mihon.core.platform.storage.AppPrivateDirectories
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

package mihon.app.storage

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import tachiyomi.core.common.storage.FolderProvider
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import java.io.File

class AndroidStorageFolderProvider(
    private val context: Context,
) : FolderProvider {

    override fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                context.stringResource(MR.strings.app_name),
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}

package tachiyomi.source.local.image

import android.content.Context
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.toUniFile

/** Keeps a folder out of the media store with a `.nomedia` file. */
class AndroidMediaScanner(private val context: Context) : MediaScanner {
    override fun exclude(directory: StorageFile) {
        DiskUtil.createNoMediaFile(directory.toUniFile(), context)
    }
}

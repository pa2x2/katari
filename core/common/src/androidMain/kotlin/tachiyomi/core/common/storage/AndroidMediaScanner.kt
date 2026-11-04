package tachiyomi.core.common.storage

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.StorageFileNames.NOMEDIA_FILE

/** Keeps a folder out of the media store with a `.nomedia` file. */
class AndroidMediaScanner(private val context: Context) : MediaScanner {
    override fun exclude(directory: StorageFile) {
        if (!directory.exists() || directory.findFile(NOMEDIA_FILE) != null) return
        directory.createFile(NOMEDIA_FILE)
        DiskUtil.scanMedia(context, Uri.parse(directory.uri))
    }
}

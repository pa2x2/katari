package eu.kanade.presentation.more.settings.screen.data

import android.app.Application
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.File

/** The root folders of the shared storage and of any mounted SD card or USB drive. */
@Inject
class AndroidStorageVolumes(private val application: Application) : StorageVolumes {
    override fun roots(): List<File> = DiskUtil.getExternalStorages(application)

    override fun space(root: File): StorageSpace = StorageSpace(
        availableBytes = DiskUtil.getAvailableStorageSpace(root),
        totalBytes = DiskUtil.getTotalStorageSpace(root),
    )
}

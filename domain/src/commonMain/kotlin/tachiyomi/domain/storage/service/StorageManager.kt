package tachiyomi.domain.storage.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import tachiyomi.core.common.storage.MediaScanner
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.StorageFileResolver
import kotlin.concurrent.Volatile

/**
 * The [StorageFolders] in the storage location the user picked. Each is made when the location lacks it; every one is
 * null until a location is picked, while it is no folder, or when it can't be made there.
 */
class StorageManager(
    private val files: StorageFileResolver,
    private val mediaScanner: MediaScanner,
    private val storagePreferences: StoragePreferences,
    scope: CoroutineScope,
) {

    @Volatile
    private var location: PickedLocation? = null

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)

    /** Sends when the user picks another storage location, after its folders are made. */
    val changes = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        storagePreferences.baseStorageDirectory.changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach {
                baseDir()?.let { parent ->
                    parent.createDirectory(StorageFolders.AUTOMATIC_BACKUPS)
                    parent.createDirectory(StorageFolders.LOCAL_SOURCE)
                    parent.createDirectory(StorageFolders.DOWNLOADS)?.let(mediaScanner::exclude)
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    fun getAutomaticBackupsDirectory(): StorageFile? = baseDir()?.createDirectory(StorageFolders.AUTOMATIC_BACKUPS)

    fun getDownloadsDirectory(): StorageFile? = baseDir()?.createDirectory(StorageFolders.DOWNLOADS)

    fun getLocalSourceDirectory(): StorageFile? = baseDir()?.createDirectory(StorageFolders.LOCAL_SOURCE)

    // Opening a picked document folder asks its provider, so the folder is kept while the location stays the same. A
    // location that is no folder isn't kept, so it is found once it is back, such as a removed SD card.
    private fun baseDir(): StorageFile? {
        val uri = storagePreferences.pickedStorageDirectory() ?: return null
        location?.takeIf { it.uri == uri }?.let { return it.folder }
        val folder = files.fromUri(uri)?.takeIf { it.isDirectory } ?: return null
        location = PickedLocation(uri, folder)
        return folder
    }

    private class PickedLocation(val uri: String, val folder: StorageFile)
}

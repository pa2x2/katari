package tachiyomi.domain.storage.service

import android.content.Context
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

class StorageManager(
    private val context: Context,
    private val storagePreferences: StoragePreferences,
    private val scope: CoroutineScope,
) {

    private var baseDir: UniFile? = pickedBaseDir()

    private val _changes: Channel<Unit> = Channel(Channel.UNLIMITED)
    val changes = _changes.receiveAsFlow()
        .shareIn(scope, SharingStarted.Lazily, 1)

    init {
        storagePreferences.baseStorageDirectory.changes()
            .drop(1)
            .distinctUntilChanged()
            .onEach {
                baseDir = pickedBaseDir()
                baseDir?.let { parent ->
                    parent.createDirectory(StorageFolders.AUTOMATIC_BACKUPS)
                    parent.createDirectory(StorageFolders.LOCAL_SOURCE)
                    parent.createDirectory(StorageFolders.DOWNLOADS).also {
                        DiskUtil.createNoMediaFile(it, context)
                    }
                }
                _changes.send(Unit)
            }
            .launchIn(scope)
    }

    private fun pickedBaseDir(): UniFile? {
        val uri = storagePreferences.pickedStorageDirectory() ?: return null
        return UniFile.fromUri(context, uri.toUri())
            .takeIf { it?.exists() == true }
    }

    fun getAutomaticBackupsDirectory(): UniFile? {
        return baseDir?.createDirectory(StorageFolders.AUTOMATIC_BACKUPS)
    }

    fun getDownloadsDirectory(): UniFile? {
        return baseDir?.createDirectory(StorageFolders.DOWNLOADS)
    }

    fun getLocalSourceDirectory(): UniFile? {
        return baseDir?.createDirectory(StorageFolders.LOCAL_SOURCE)
    }
}

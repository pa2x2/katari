package eu.kanade.presentation.more.settings.screen.data

import android.app.Application
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.domain.storage.DocumentUri

/** Storage locations are the document tree URIs the folder picker returns, opened through UniFile. */
@Inject
class AndroidStorageLocationPaths(private val application: Application) : StorageLocationPaths {
    override fun locationOf(folder: DocumentUri): String? =
        UniFile.fromUri(application, folder.value.toUri())?.uri?.toString()

    override fun displayablePath(location: String): String? =
        UniFile.fromUri(application, location.toUri())?.displayablePath
}

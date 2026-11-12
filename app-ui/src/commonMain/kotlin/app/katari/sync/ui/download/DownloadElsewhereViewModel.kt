package app.katari.sync.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.downloadrequest.ChapterToRequest
import app.katari.sync.client.downloadrequest.DownloadRequestSending
import app.katari.sync.client.downloadrequest.DownloadTarget
import app.katari.sync.client.downloadrequest.DownloadTargets
import app.katari.sync.client.identity.SyncedEntries
import app.katari.sync.protocol.device.LinkedDevice
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.extension.ExtensionManagement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext

/** The account's other devices as [sourceId]'s chapters could go to them, with the name of that source's extension. */
data class DownloadElsewhereDevices(val targets: List<DownloadTarget>, val extensionName: String?)

/**
 * Asking the account's other devices to download chapters: which devices could, which entries they can be asked about,
 * and sending the request. A request is sent in the app's scope, so leaving the screen right after picking a device
 * doesn't drop it.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class DownloadElsewhereViewModel(
    private val targets: DownloadTargets,
    private val sending: DownloadRequestSending,
    private val extensions: ExtensionManagement,
    syncedEntries: SyncedEntries,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ViewModel() {
    /** The devices before an entry is picked, so without telling which lack its extension. */
    val devices: StateFlow<List<DownloadTarget>> =
        targets.of(pkgName = null).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The entries another device can be asked about. */
    val syncing: StateFlow<Set<Long>> =
        syncedEntries.syncing().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** The devices for chapters of [sourceId], null when they come from several sources. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun devicesFor(sourceId: Long?): Flow<DownloadElsewhereDevices> = extensions.loaded
        .map { loaded ->
            sourceId?.let { id -> loaded.firstOrNull { extension -> extension.sources.any { it.id == id } } }
        }
        .distinctUntilChanged()
        .flatMapLatest { extension ->
            targets.of(extension?.pkgName).map { DownloadElsewhereDevices(it, extension?.name) }
        }

    suspend fun currentDevicesFor(sourceId: Long?): DownloadElsewhereDevices = devicesFor(sourceId).first()

    /** Asks [device] to download the [chapters], then calls [onSent] if any of them were asked for. */
    fun send(device: LinkedDevice, chapters: suspend () -> List<ChapterToRequest>, onSent: () -> Unit) {
        appScope.launchIO {
            if (sending.send(device.id, chapters()) > 0) withUIContext { onSent() }
        }
    }
}

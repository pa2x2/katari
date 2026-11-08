package app.katari.desktop.translation.server

import app.katari.desktop.storage.DesktopDirectories
import app.katari.translation.provider.server.ServerConnectionId
import app.katari.translation.provider.server.ServerConnectionPreferenceStores
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import java.util.concurrent.ConcurrentHashMap

/** Each connection's settings in a file of the configuration folder named after it, as Android names its file. */
@Inject
class FileServerConnectionPreferenceStores(
    private val directories: DesktopDirectories,
) : ServerConnectionPreferenceStores {
    private val stores = ConcurrentHashMap<String, PreferenceStore>()

    override fun store(connection: ServerConnectionId): PreferenceStore =
        stores.getOrPut(connection.preferencesName) {
            DesktopPreferenceStore(directories.configuration.resolve("${connection.preferencesName}.json"))
        }
}

package mihon.desktop.translation.server

import dev.zacsweers.metro.Inject
import mihon.desktop.storage.DesktopDirectories
import mihon.translation.provider.server.ServerConnectionId
import mihon.translation.provider.server.ServerConnectionPreferenceStores
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

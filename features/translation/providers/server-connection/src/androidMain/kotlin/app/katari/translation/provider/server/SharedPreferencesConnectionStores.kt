package app.katari.translation.provider.server

import android.content.Context
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import java.util.concurrent.ConcurrentHashMap

/** Each connection's settings in the shared preferences file named after it, which also holds its encrypted key. */
internal class SharedPreferencesConnectionStores(private val context: Context) : ServerConnectionPreferenceStores {
    private val stores = ConcurrentHashMap<String, PreferenceStore>()

    override fun store(connection: ServerConnectionId): PreferenceStore =
        stores.getOrPut(connection.preferencesName) {
            AndroidPreferenceStore(
                context = context,
                sharedPreferences = context.getSharedPreferences(connection.preferencesName, Context.MODE_PRIVATE),
            )
        }
}

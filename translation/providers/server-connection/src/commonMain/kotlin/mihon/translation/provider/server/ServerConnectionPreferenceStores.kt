package mihon.translation.provider.server

import tachiyomi.core.common.preference.PreferenceStore

/** The platform's file of each server connection's settings, the ones that aren't secret. */
fun interface ServerConnectionPreferenceStores {
    /** The same store for every call with the same [ServerConnectionId.preferencesName]. */
    fun store(connection: ServerConnectionId): PreferenceStore
}

package app.katari.translation.provider.server

import app.katari.core.common.platform.PlatformFacility
import tachiyomi.core.common.preference.PreferenceStore

/** The platform's file of each server connection's settings, the ones that aren't secret. */
@PlatformFacility
fun interface ServerConnectionPreferenceStores {
    /** The same store for every call with the same [ServerConnectionId.preferencesName]. */
    fun store(connection: ServerConnectionId): PreferenceStore
}

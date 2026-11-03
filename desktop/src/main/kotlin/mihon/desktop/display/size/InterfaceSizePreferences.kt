package mihon.desktop.display.size

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** The interface size, on the profile's store, where Android keeps its display settings such as Tablet UI. */
class InterfaceSizePreferences(store: PreferenceStore) {
    /** Percent of the session's scale; a value from outside [InterfaceSize.RANGE], such as a restored one, is clamped. */
    val interfaceSize: Preference<Int> = store.getInt("interface_size", InterfaceSize.DEFAULT)
}

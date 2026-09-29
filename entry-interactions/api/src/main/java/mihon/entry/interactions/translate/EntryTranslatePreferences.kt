package mihon.entry.interactions.translate

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** When chapters are translated in the background without the user queuing each one, and when that work may run. */
class EntryTranslatePreferences(
    preferenceStore: PreferenceStore,
) {
    /** Chapters that automatic download fetches are translated as well. */
    val translateNewChapters: Preference<Boolean> = preferenceStore.getBoolean("translate_new_chapters", false)

    /** How many unread chapters after the one being read are translated ahead; 0 turns it off. */
    val translateAheadWhileReading: Preference<Int> = preferenceStore.getInt("translate_ahead_while_reading", 0)

    val onlyWhileCharging: Preference<Boolean> = preferenceStore.getBoolean("translate_only_while_charging", false)

    /** Chapters translated by an engine that sends text over the network wait for Wi-Fi. */
    val onlyOverWifi: Preference<Boolean> = preferenceStore.getBoolean("translate_only_over_wifi", true)
}

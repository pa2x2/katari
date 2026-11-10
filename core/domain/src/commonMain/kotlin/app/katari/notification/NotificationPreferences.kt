package app.katari.notification

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Which kinds of notification show, where the app keeps that choice rather than the system: on the desktop. Android
 * keeps it in its own settings for each channel and never reads these.
 */
class NotificationPreferences(private val preferenceStore: PreferenceStore) {
    fun shows(
        kind: NotificationKind,
    ): Preference<Boolean> = preferenceStore.getBoolean("notification_shown_${kind.id}", true)
}

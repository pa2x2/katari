package app.katari.notification

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Whether the app's notifications show at all, and which kinds, where the app keeps that choice rather than the
 * system: on the desktop. Android keeps it in its own settings for the app and each channel and never reads these.
 */
class NotificationPreferences(private val preferenceStore: PreferenceStore) {
    /** Off, no notification shows, whatever [shows] says of its kind. */
    val enabled: Preference<Boolean> = preferenceStore.getBoolean("notifications_enabled", true)

    fun shows(
        kind: NotificationKind,
    ): Preference<Boolean> = preferenceStore.getBoolean("notification_shown_${kind.id}", true)
}

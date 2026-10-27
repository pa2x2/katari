package mihon.feature.appupdate

import mihon.feature.appupdate.check.AppUpdateChannel
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

/** App-wide, not per profile: there is one installed app to update. */
internal class AppUpdatePreferences(preferenceStore: PreferenceStore) {

    val checkOnLaunch: Preference<Boolean> = preferenceStore.getBoolean("app_update_check_on_launch", true)

    val channel: Preference<AppUpdateChannel> = preferenceStore.getEnum("app_update_channel", AppUpdateChannel.STABLE)

    /** The version whose prompt the user skipped; automatic checks don't offer it again. Empty for none. */
    val skippedVersion: Preference<String> = preferenceStore.getString(Preference.appStateKey("app_update_skipped"), "")

    /** The optional mpv add-on version whose prompt the user skipped. Empty for none. */
    val skippedMpvAddonVersion: Preference<String> =
        preferenceStore.getString(Preference.appStateKey("mpv_addon_update_skipped"), "")

    /** Epoch milliseconds of the last check that reached GitHub; 0 for never. */
    val lastCheckedAt: Preference<Long> = preferenceStore.getLong(Preference.appStateKey("app_update_last_checked"), 0)
}

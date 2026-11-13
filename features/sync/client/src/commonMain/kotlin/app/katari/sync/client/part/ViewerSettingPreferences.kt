package app.katari.sync.client.part

/**
 * Which app setting a title's own reader or player value stands in for, which only the app's viewers know: a title's
 * value of a setting syncs only while the setting itself does on this device (D005).
 */
fun interface ViewerSettingPreferences {
    /**
     * The preference key of the setting that [settingKey] of the viewer settings [providerId] is a title's own value
     * of; null when this device has no such viewer setting, whose title values then follow Title choices alone.
     */
    fun preferenceKey(providerId: String, settingKey: String): String?
}

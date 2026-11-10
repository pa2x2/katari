package app.katari.sync.client.setting.scope

/**
 * `SecurityPreferences`, but for hiding notification content, which `SystemScopes` marks with the notification
 * settings. Hiding content from screenshots and the task switcher syncs. The app lock stays: it
 * unlocks with what this device has, a phone's screen lock or biometrics or a computer's sign-in, which another device
 * may not have.
 */
internal val SecurityScopes = PreferenceScopeTable(SettingsArea.SECURITY) {
    person("secure_screen_v2")
    device(DeviceBound.SECURITY, "use_biometric_lock", "lock_app_after")
}

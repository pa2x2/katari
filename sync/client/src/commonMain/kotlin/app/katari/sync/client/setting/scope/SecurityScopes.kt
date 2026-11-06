package app.katari.sync.client.setting.scope

/**
 * `SecurityPreferences`. Hiding content from screenshots and the task switcher syncs. The app lock stays: it
 * unlocks with this device's screen lock or biometrics, which another device may not have, and the desktop has no
 * authenticator at all.
 */
internal val SecurityScopes = PreferenceScopeTable {
    person("secure_screen_v2")
    device("use_biometric_lock", "lock_app_after", "hide_notification_content")
}

package app.katari.sync.client.setting.scope

/**
 * The device itself: `NetworkPreferences`, `PrivacyPreferences`, `ProgressiveImagePreferences`, `AppUpdatePreferences`,
 * `NotificationPreferences`, the notification setting of `SecurityPreferences`, Android's `BasePreferences` and
 * extension installer, and the desktop's `TouchpadPreferences` and `TrayPreferences`.
 */
internal val SystemScopes = PreferenceScopeTable {
    device(DeviceBound.DIAGNOSTICS, "redact_sensitive_headers", "verbose_logging")
    device(DeviceBound.NETWORK, "doh_provider", "default_user_agent")
    device(DeviceBound.DIAGNOSTICS, "crashlytics", "analytics")
    device(DeviceBound.HARDWARE, "progressive_image_loading")
    device(DeviceBound.APP, "app_update_check_on_launch", "app_update_channel")
    device(DeviceBound.HARDWARE, "pref_hardware_bitmap_threshold")
    device(DeviceBound.INSTALLED, "extension_installer")

    // Whether this desktop shows notifications, and which kinds, as Android keeps per device for the app and each channel
    device(DeviceBound.NOTIFICATIONS, "notifications_enabled")
    device(DeviceBound.NOTIFICATIONS, "hide_notification_content")
    devicePrefix(DeviceBound.NOTIFICATIONS, "notification_shown_")

    // Desktop: TouchpadPreferences, for this computer's touchpad and the system's own setting for it
    device(DeviceBound.HARDWARE, "touchpad_natural_scrolling")

    // Desktop: TrayPreferences, which work only where this computer's desktop shows a tray
    device(DeviceBound.SYSTEM, "tray_keep_running")
}

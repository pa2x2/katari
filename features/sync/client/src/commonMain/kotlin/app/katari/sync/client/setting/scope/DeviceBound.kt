package app.katari.sync.client.setting.scope

/** Why a setting only works on the device it was set on, which the screens that list it say. */
enum class DeviceBound {
    /** It depends on what this device's system offers, such as dynamic colour or the per-app language. */
    SYSTEM,

    /** It names engines, voices, providers or servers installed or set up on this device. */
    INSTALLED,

    /** It unlocks with this device's own screen lock or sign-in. */
    SECURITY,
    STORAGE,

    /** It follows this device's network or power, such as Wi-Fi-only rules. */
    NETWORK,
    NOTIFICATIONS,

    /** It is made for this device's screen or hardware, such as e-ink refreshes or its colour profile. */
    HARDWARE,
    DIAGNOSTICS,
    DOWNLOADS,

    /** It says how this device syncs. */
    SYNC,

    /** The app keeps it for itself: hints shown, drafts, keys of earlier versions. */
    APP,
}

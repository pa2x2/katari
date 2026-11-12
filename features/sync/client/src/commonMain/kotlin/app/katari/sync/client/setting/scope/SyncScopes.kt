package app.katari.sync.client.setting.scope

/** How this device syncs: `SyncPreferences`. */
internal val SyncScopes = PreferenceScopeTable {
    device(
        DeviceBound.SYNC,
        "sync_use_mobile_data",
        "sync_in_background",
        "sync_background_interval",
        "sync_instant",
        "sync_paused",
        "sync_changes_on_mobile_data",
    )
}

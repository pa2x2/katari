package app.katari.sync.client.setting.scope

/** How this device syncs: `SyncPreferences`. */
internal val SyncScopes = PreferenceScopeTable {
    device("sync_use_mobile_data", "sync_in_background")
}

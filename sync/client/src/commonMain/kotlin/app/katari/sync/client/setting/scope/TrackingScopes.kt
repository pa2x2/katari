package app.katari.sync.client.setting.scope

/**
 * `TrackPreferences`, which live in the profile's private store. A person signs in to each tracker on each device, and
 * the score format and what updates a tracker belong to that sign-in, so none of them sync; the tracker bindings do.
 */
internal val TrackingScopes = PreferenceScopeTable {
    device(
        "anilist_score_type",
        "kitsu_score_type",
        "mangabaka_score_type",
        "pref_auto_update_manga_sync_key",
        "pref_auto_update_manga_on_mark_read",
    )
}

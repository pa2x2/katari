package app.katari.sync.client.setting.scope

/** `StatisticsPreferences`, which sync with the statistics themselves. */
internal val StatisticsScopes = PreferenceScopeTable(SettingsArea.STATISTICS) {
    person("statistics_daily_goal_minutes", "statistics_recap_hidden_entries", "statistics_recap_include_nsfw")
    // Which cards each statistics tab shows, by tab
    personPrefix("statistics_cards_")
    device(DeviceBound.NOTIFICATIONS, "statistics_monthly_recap_notification", "statistics_yearly_recap_notification")
}

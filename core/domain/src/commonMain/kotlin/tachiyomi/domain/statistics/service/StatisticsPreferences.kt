package tachiyomi.domain.statistics.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class StatisticsPreferences(
    private val preferenceStore: PreferenceStore,
) {

    val selectedRange: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_selected_range"),
        DEFAULT_RANGE,
    )

    val selectedType: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_selected_type"),
        OVERVIEW_TYPE,
    )

    fun cardLayout(tab: String): Preference<String> = preferenceStore.getString("statistics_cards_$tab", "")

    /** Minutes of reading a day counts toward; 0 when no goal is set. */
    val dailyGoalMinutes: Preference<Int> = preferenceStore.getInt("statistics_daily_goal_minutes", 0)

    val monthlyRecapNotification: Preference<Boolean> = preferenceStore.getBoolean(
        "statistics_monthly_recap_notification",
        false,
    )

    /** Last month, `yyyy-MM`, a recap notification was handled for; empty before the first. */
    val lastRecapNotificationMonth: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_last_recap_notification_month"),
        "",
    )

    val yearlyRecapNotification: Preference<Boolean> = preferenceStore.getBoolean(
        "statistics_yearly_recap_notification",
        true,
    )

    /** The last year recap edition, such as `2026-so-far` or `2026`, a notification was handled for. */
    val lastYearRecapNotification: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_last_year_recap_notification"),
        "",
    )

    /** The last year recap edition opened, so the new one is marked until it's seen. */
    val lastOpenedYearRecap: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_last_opened_year_recap"),
        "",
    )

    /** Titles hidden from recaps by hand, as `year:entryId`; a title hidden once stays hidden for that year. */
    val recapHiddenEntries: Preference<Set<String>> = preferenceStore.getStringSet(
        "statistics_recap_hidden_entries",
        emptySet(),
    )

    /** Whether recaps may show titles from extensions marked 18+. */
    val recapIncludeNsfw: Preference<Boolean> = preferenceStore.getBoolean("statistics_recap_include_nsfw", false)

    companion object {
        const val DEFAULT_RANGE = "THIRTY_DAYS"
        const val OVERVIEW_TYPE = ""
    }
}

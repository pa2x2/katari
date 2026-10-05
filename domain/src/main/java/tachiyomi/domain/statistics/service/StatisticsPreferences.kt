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

    companion object {
        const val DEFAULT_RANGE = "THIRTY_DAYS"
        const val OVERVIEW_TYPE = ""
    }
}

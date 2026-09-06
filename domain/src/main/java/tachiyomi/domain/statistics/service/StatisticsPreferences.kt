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

    companion object {
        const val DEFAULT_RANGE = "THIRTY_DAYS"
        const val OVERVIEW_TYPE = ""
    }
}

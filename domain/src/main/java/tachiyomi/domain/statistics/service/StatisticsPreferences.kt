package tachiyomi.domain.statistics.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class StatisticsPreferences(
    preferenceStore: PreferenceStore,
) {

    val selectedRange: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_selected_range"),
        DEFAULT_RANGE,
    )

    val selectedType: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("statistics_selected_type"),
        OVERVIEW_TYPE,
    )

    companion object {
        const val DEFAULT_RANGE = "THIRTY_DAYS"
        const val OVERVIEW_TYPE = ""
    }
}

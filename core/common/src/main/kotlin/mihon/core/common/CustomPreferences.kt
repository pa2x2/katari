package mihon.core.common

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR

class CustomPreferences(
    preferenceStore: PreferenceStore,
) {
    companion object {
        const val HOME_SCREEN_STARTUP_TAB_KEY = "home_screen_startup_tab"
        const val HOME_SCREEN_TABS_KEY = "home_screen_tabs"
        const val HOME_SCREEN_TAB_ORDER_KEY = "home_screen_tab_order"
        const val ENABLE_FEEDS_KEY = "enable_feeds"
        const val BROWSE_LONG_PRESS_ACTION_KEY = "browse_long_press_action"

        val profileKeys = setOf(
            HOME_SCREEN_STARTUP_TAB_KEY,
            HOME_SCREEN_TABS_KEY,
            HOME_SCREEN_TAB_ORDER_KEY,
            ENABLE_FEEDS_KEY,
            BROWSE_LONG_PRESS_ACTION_KEY,
        )
    }

    val homeScreenStartupTab: Preference<HomeScreenTabs> = preferenceStore.getEnum(
        HOME_SCREEN_STARTUP_TAB_KEY,
        HomeScreenTabs.Library,
    )

    val homeScreenTabs: Preference<Set<String>> = preferenceStore.getStringSet(
        HOME_SCREEN_TABS_KEY,
        defaultHomeScreenTabs(),
    )

    val homeScreenTabOrder: Preference<List<HomeScreenTabs>> = preferenceStore.getObjectFromString(
        HOME_SCREEN_TAB_ORDER_KEY,
        defaultHomeScreenTabOrder(),
        serializer = { it.toHomeScreenTabOrderPreferenceValue() },
        deserializer = { it.toHomeScreenTabOrder() },
    )

    val enableFeeds: Preference<Boolean> = preferenceStore.getBoolean(
        ENABLE_FEEDS_KEY,
        true,
    )

    val browseLongPressAction: Preference<BrowseLongPressAction> = preferenceStore.getObjectFromString(
        BROWSE_LONG_PRESS_ACTION_KEY,
        BrowseLongPressAction.LIBRARY_ACTION,
        serializer = { it.name },
        deserializer = {
            when (it) {
                "MANGA_PREVIEW" -> BrowseLongPressAction.PREVIEW
                else -> runCatching { BrowseLongPressAction.valueOf(it) }
                    .getOrDefault(BrowseLongPressAction.LIBRARY_ACTION)
            }
        },
    )

    enum class BrowseLongPressAction(val titleRes: StringResource) {
        LIBRARY_ACTION(MR.strings.pref_browse_long_press_action_library_action),
        PREVIEW(MR.strings.pref_browse_long_press_action_preview),
    }
}

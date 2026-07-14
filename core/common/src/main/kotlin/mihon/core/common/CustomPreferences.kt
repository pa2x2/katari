package mihon.core.common

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR

class CustomPreferences(
    private val preferenceStore: PreferenceStore,
) {
    companion object {
        const val HOME_SCREEN_STARTUP_TAB_KEY = "home_screen_startup_tab"
        const val HOME_SCREEN_TABS_KEY = "home_screen_tabs"
        const val HOME_SCREEN_TAB_ORDER_KEY = "home_screen_tab_order"
        const val ENABLE_FEEDS_KEY = "enable_feeds"
        const val BROWSE_LONG_PRESS_ACTION_KEY = "browse_long_press_action"
        const val BROWSE_LONG_PRESS_ACTION_OVERRIDES_KEY = "browse_long_press_action_overrides"

        val profileKeys = setOf(
            HOME_SCREEN_STARTUP_TAB_KEY,
            HOME_SCREEN_TABS_KEY,
            HOME_SCREEN_TAB_ORDER_KEY,
            ENABLE_FEEDS_KEY,
            BROWSE_LONG_PRESS_ACTION_KEY,
            BROWSE_LONG_PRESS_ACTION_OVERRIDES_KEY,
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

    val browseLongPressActionPriority: Preference<List<BrowseLongPressAction>> = preferenceStore.getObjectFromString(
        BROWSE_LONG_PRESS_ACTION_KEY,
        defaultBrowseLongPressActionPriority(),
        serializer = { it.toBrowseLongPressActionPriorityPreferenceValue() },
        deserializer = { it.toBrowseLongPressActionPriority() },
    )

    val browseLongPressActionOverrides: Preference<Map<Long, List<BrowseLongPressAction>>> =
        preferenceStore.getObjectFromString(
            BROWSE_LONG_PRESS_ACTION_OVERRIDES_KEY,
            emptyMap(),
            serializer = { it.toBrowseLongPressActionOverridesPreferenceValue() },
            deserializer = { it.toBrowseLongPressActionOverrides() },
        )

    fun browseLongPressActionPriority(sourceId: Long): List<BrowseLongPressAction> {
        return browseLongPressActionPriorityForSource(
            sourceId = sourceId,
            defaultPriority = browseLongPressActionPriority.get(),
            overrides = browseLongPressActionOverrides.get(),
        )
    }

    fun setBrowseLongPressActionOverride(sourceId: Long, priority: Collection<BrowseLongPressAction>) {
        browseLongPressActionOverrides.set(
            browseLongPressActionOverrides.get() +
                (sourceId to sanitizeBrowseLongPressActionPriority(priority)),
        )
    }

    fun clearBrowseLongPressActionOverride(sourceId: Long) {
        browseLongPressActionOverrides.set(browseLongPressActionOverrides.get() - sourceId)
    }

    enum class BrowseLongPressAction(val titleRes: StringResource) {
        LIBRARY_ACTION(MR.strings.pref_browse_long_press_action_library_action),
        PREVIEW(MR.strings.pref_browse_long_press_action_preview),
        IMMERSIVE(MR.strings.pref_browse_long_press_action_immersive),
    }
}

private val browseLongPressActions = listOf(
    CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
    CustomPreferences.BrowseLongPressAction.PREVIEW,
    CustomPreferences.BrowseLongPressAction.IMMERSIVE,
)

fun defaultBrowseLongPressActionPriority(): List<CustomPreferences.BrowseLongPressAction> {
    return browseLongPressActions.toList()
}

fun browseLongPressActionPriorityForSource(
    sourceId: Long,
    defaultPriority: List<CustomPreferences.BrowseLongPressAction>,
    overrides: Map<Long, List<CustomPreferences.BrowseLongPressAction>>,
): List<CustomPreferences.BrowseLongPressAction> {
    return overrides[sourceId] ?: defaultPriority
}

fun sanitizeBrowseLongPressActionPriority(
    priority: Collection<CustomPreferences.BrowseLongPressAction>,
): List<CustomPreferences.BrowseLongPressAction> {
    return buildList {
        priority.distinct().forEach { action ->
            if (action in browseLongPressActions) add(action)
        }
        browseLongPressActions.forEach { action ->
            if (action !in this) add(action)
        }
    }
}

fun Collection<CustomPreferences.BrowseLongPressAction>.toBrowseLongPressActionPriorityPreferenceValue(): String {
    return sanitizeBrowseLongPressActionPriority(this).joinToString(",") { it.name }
}

fun String.toBrowseLongPressActionPriority(): List<CustomPreferences.BrowseLongPressAction> {
    if (isBlank()) return defaultBrowseLongPressActionPriority()

    return sanitizeBrowseLongPressActionPriority(
        split(',').mapNotNull { serializedAction ->
            when (serializedAction) {
                "MANGA_PREVIEW" -> CustomPreferences.BrowseLongPressAction.PREVIEW
                else -> CustomPreferences.BrowseLongPressAction.entries.find { it.name == serializedAction }
            }
        },
    )
}

fun Map<Long, List<CustomPreferences.BrowseLongPressAction>>.toBrowseLongPressActionOverridesPreferenceValue(): String {
    return entries
        .sortedBy { it.key }
        .joinToString(";") { (sourceId, priority) ->
            "$sourceId:${priority.toBrowseLongPressActionPriorityPreferenceValue()}"
        }
}

fun String.toBrowseLongPressActionOverrides(): Map<Long, List<CustomPreferences.BrowseLongPressAction>> {
    if (isBlank()) return emptyMap()

    return split(';').mapNotNull { serializedOverride ->
        val sourceId = serializedOverride.substringBefore(':').toLongOrNull() ?: return@mapNotNull null
        val serializedPriority = serializedOverride.substringAfter(':', missingDelimiterValue = "")
        val parsedActions = serializedPriority.split(',').mapNotNull { serializedAction ->
            CustomPreferences.BrowseLongPressAction.entries.find { it.name == serializedAction }
        }
        if (parsedActions.isEmpty()) return@mapNotNull null
        sourceId to sanitizeBrowseLongPressActionPriority(parsedActions)
    }.toMap()
}

package mihon.core.common

enum class HomeScreenTabs {
    Library,
    Updates,
    History,
    Browse,
    More,
    Profiles,
}

val homeScreenTabOrder = listOf(
    HomeScreenTabs.Library,
    HomeScreenTabs.Updates,
    HomeScreenTabs.History,
    HomeScreenTabs.Browse,
    HomeScreenTabs.More,
    HomeScreenTabs.Profiles,
)

val homeScreenContentTabOrder = homeScreenTabOrder.filterNot { it == HomeScreenTabs.Profiles }

fun defaultHomeScreenTabOrder(): List<HomeScreenTabs> {
    return homeScreenTabOrder.toList()
}

fun Set<String>.toHomeScreenTabs(): Set<HomeScreenTabs> {
    return mapNotNullTo(linkedSetOf()) { entry ->
        HomeScreenTabs.entries.find { it.name == entry }
    }
}

fun sanitizeHomeScreenTabOrder(tabOrder: Collection<HomeScreenTabs>): List<HomeScreenTabs> {
    return buildList {
        tabOrder.distinct().forEach { tab ->
            if (tab in homeScreenTabOrder) {
                add(tab)
            }
        }
        homeScreenTabOrder.forEach { tab ->
            if (tab !in this) {
                add(tab)
            }
        }
    }
}

fun sanitizeHomeScreenTabs(
    tabs: Set<HomeScreenTabs>,
    tabOrder: Collection<HomeScreenTabs> = homeScreenTabOrder,
): List<HomeScreenTabs> {
    return sanitizeHomeScreenTabOrder(tabOrder).filter { it == HomeScreenTabs.More || it in tabs }
}

fun resolveVisibleHomeScreenTabs(
    tabs: Set<HomeScreenTabs>,
    tabOrder: Collection<HomeScreenTabs> = homeScreenTabOrder,
    showProfilesTab: Boolean,
): List<HomeScreenTabs> {
    return sanitizeHomeScreenTabs(tabs, tabOrder).filter { it != HomeScreenTabs.Profiles || showProfilesTab }
}

fun defaultHomeScreenTabs(): Set<String> {
    return homeScreenContentTabOrder.mapTo(linkedSetOf()) { it.name }
}

fun Collection<HomeScreenTabs>.toHomeScreenTabPreferenceValue(): Set<String> {
    return mapTo(linkedSetOf()) { it.name }
}

fun Collection<HomeScreenTabs>.toHomeScreenTabOrderPreferenceValue(): String {
    return sanitizeHomeScreenTabOrder(this).joinToString(",") { it.name }
}

fun String.toHomeScreenTabOrder(): List<HomeScreenTabs> {
    if (isBlank()) return defaultHomeScreenTabOrder()

    return sanitizeHomeScreenTabOrder(
        split(',').mapNotNull { entry ->
            HomeScreenTabs.entries.find { it.name == entry }
        },
    )
}

fun resolveHomeScreenTab(
    requestedTab: HomeScreenTabs,
    enabledTabs: Collection<HomeScreenTabs>,
    tabOrder: Collection<HomeScreenTabs> = homeScreenTabOrder,
): HomeScreenTabs {
    val sanitizedTabOrder = sanitizeHomeScreenTabOrder(tabOrder)
    val sanitizedTabs = sanitizedTabOrder.filter { it in enabledTabs }
    return when {
        requestedTab in sanitizedTabs -> requestedTab
        HomeScreenTabs.Library in sanitizedTabs -> HomeScreenTabs.Library
        else -> {
            val requestedIndex = sanitizedTabOrder.indexOf(requestedTab)
            sanitizedTabs.firstOrNull { sanitizedTabOrder.indexOf(it) > requestedIndex }
                ?: sanitizedTabs.firstOrNull()
                ?: HomeScreenTabs.More
        }
    }
}

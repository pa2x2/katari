package mihon.core.common.navigation

import mihon.core.common.HomeScreenTabs
import mihon.core.common.defaultHomeScreenTabs
import mihon.core.common.homeScreenTabOrder
import mihon.core.common.sanitizeHomeScreenTabOrder
import mihon.core.common.sanitizeHomeScreenTabs

const val MAX_HOME_NAVIGATION_BAR_ITEMS = 5
const val MAX_HOME_NAVIGATION_PRIMARY_TABS_WITH_OVERFLOW = MAX_HOME_NAVIGATION_BAR_ITEMS - 1

enum class HomeNavigationSection {
    Primary,
    Overflow,
    Hidden,
}

sealed interface HomeNavigationMoveResult {
    data class Moved(val configuration: HomeNavigationConfiguration) : HomeNavigationMoveResult
    data object PrimaryRequired : HomeNavigationMoveResult
    data object MoreRequired : HomeNavigationMoveResult
}

data class HomeNavigationConfiguration(
    val primaryTabs: List<HomeScreenTabs>,
    val overflowTabs: List<HomeScreenTabs>,
    val hiddenTabs: List<HomeScreenTabs>,
) {
    val enabledTabs: List<HomeScreenTabs>
        get() = primaryTabs + overflowTabs

    val tabOrder: List<HomeScreenTabs>
        get() = primaryTabs + overflowTabs + hiddenTabs
}

fun resolveHomeNavigationConfiguration(
    enabledTabs: Set<HomeScreenTabs>,
    tabOrder: Collection<HomeScreenTabs>,
    primaryTabs: Collection<HomeScreenTabs>,
): HomeNavigationConfiguration {
    val resolvedOrder = sanitizeHomeScreenTabOrder(tabOrder)
    val resolvedEnabledTabs = sanitizeHomeScreenTabs(enabledTabs, resolvedOrder)
    val primaryCandidates = if (primaryTabs.isEmpty()) {
        val defaultPrimaryCount = if (resolvedEnabledTabs.size <= MAX_HOME_NAVIGATION_BAR_ITEMS) {
            MAX_HOME_NAVIGATION_BAR_ITEMS
        } else {
            MAX_HOME_NAVIGATION_PRIMARY_TABS_WITH_OVERFLOW
        }
        resolvedEnabledTabs.take(defaultPrimaryCount)
    } else {
        resolvedOrder
            .filter { it in primaryTabs && it in resolvedEnabledTabs }
            .take(MAX_HOME_NAVIGATION_BAR_ITEMS)
    }
    val primaryCapacity = if (
        primaryCandidates.size == resolvedEnabledTabs.size &&
        resolvedEnabledTabs.size <= MAX_HOME_NAVIGATION_BAR_ITEMS
    ) {
        MAX_HOME_NAVIGATION_BAR_ITEMS
    } else {
        MAX_HOME_NAVIGATION_PRIMARY_TABS_WITH_OVERFLOW
    }
    val resolvedPrimaryTabs = primaryCandidates.take(primaryCapacity).ifEmpty { resolvedEnabledTabs.take(1) }

    return HomeNavigationConfiguration(
        primaryTabs = resolvedPrimaryTabs,
        overflowTabs = resolvedEnabledTabs.filterNot { it in resolvedPrimaryTabs },
        hiddenTabs = resolvedOrder.filterNot { it in resolvedEnabledTabs },
    )
}

fun defaultHomeNavigationConfiguration(): HomeNavigationConfiguration {
    return resolveHomeNavigationConfiguration(
        enabledTabs = defaultHomeScreenTabs().mapTo(linkedSetOf()) { HomeScreenTabs.valueOf(it) },
        tabOrder = homeScreenTabOrder,
        primaryTabs = emptyList(),
    )
}

fun HomeNavigationConfiguration.withVisibleProfiles(showProfilesTab: Boolean): HomeNavigationConfiguration {
    if (showProfilesTab) return this

    val visiblePrimaryTabs = primaryTabs.filterNot { it == HomeScreenTabs.Profiles }
    val visibleOverflowTabs = overflowTabs.filterNot { it == HomeScreenTabs.Profiles }
    if (visiblePrimaryTabs.isNotEmpty()) {
        return copy(
            primaryTabs = visiblePrimaryTabs,
            overflowTabs = visibleOverflowTabs,
        )
    }

    val promotedTab = visibleOverflowTabs.firstOrNull()
    return copy(
        primaryTabs = listOfNotNull(promotedTab),
        overflowTabs = visibleOverflowTabs.filterNot { it == promotedTab },
    )
}

fun HomeNavigationConfiguration.move(
    tab: HomeScreenTabs,
    targetSection: HomeNavigationSection,
    targetIndex: Int,
): HomeNavigationMoveResult {
    if (targetSection == HomeNavigationSection.Hidden && tab == HomeScreenTabs.More) {
        return HomeNavigationMoveResult.MoreRequired
    }
    if (tab in primaryTabs && primaryTabs.size == 1 && targetSection != HomeNavigationSection.Primary) {
        return HomeNavigationMoveResult.PrimaryRequired
    }

    val sourceSection = when (tab) {
        in primaryTabs -> HomeNavigationSection.Primary
        in overflowTabs -> HomeNavigationSection.Overflow
        else -> HomeNavigationSection.Hidden
    }
    val sourceIndex = when (sourceSection) {
        HomeNavigationSection.Primary -> primaryTabs.indexOf(tab)
        HomeNavigationSection.Overflow -> overflowTabs.indexOf(tab)
        HomeNavigationSection.Hidden -> hiddenTabs.indexOf(tab)
    }
    val adjustedTargetIndex = if (sourceSection == targetSection && sourceIndex < targetIndex) {
        targetIndex - 1
    } else {
        targetIndex
    }

    val primary = primaryTabs.filterNot { it == tab }.toMutableList()
    val overflow = overflowTabs.filterNot { it == tab }.toMutableList()
    val hidden = hiddenTabs.filterNot { it == tab }.toMutableList()

    when (targetSection) {
        HomeNavigationSection.Primary -> {
            val insertionIndex = adjustedTargetIndex.coerceIn(0, primary.size)
            primary.add(insertionIndex, tab)
            val primaryCapacity = if (overflow.isEmpty() && primary.size <= MAX_HOME_NAVIGATION_BAR_ITEMS) {
                MAX_HOME_NAVIGATION_BAR_ITEMS
            } else {
                MAX_HOME_NAVIGATION_PRIMARY_TABS_WITH_OVERFLOW
            }
            if (primary.size > primaryCapacity) {
                val displacedTabs = primary.subList(primaryCapacity, primary.size).toList()
                primary.subList(primaryCapacity, primary.size).clear()
                overflow.addAll(0, displacedTabs)
            }
        }
        HomeNavigationSection.Overflow -> overflow.add(adjustedTargetIndex.coerceIn(0, overflow.size), tab)
        HomeNavigationSection.Hidden -> hidden.add(adjustedTargetIndex.coerceIn(0, hidden.size), tab)
    }

    return HomeNavigationMoveResult.Moved(
        HomeNavigationConfiguration(
            primaryTabs = primary,
            overflowTabs = overflow,
            hiddenTabs = hidden,
        ),
    )
}

fun sanitizeHomeNavigationPrimaryTabs(primaryTabs: Collection<HomeScreenTabs>): List<HomeScreenTabs> {
    return primaryTabs
        .distinct()
        .filter { it in homeScreenTabOrder }
        .take(MAX_HOME_NAVIGATION_BAR_ITEMS)
}

fun Collection<HomeScreenTabs>.toHomeNavigationPrimaryTabsPreferenceValue(): String {
    return sanitizeHomeNavigationPrimaryTabs(this).joinToString(",") { it.name }
}

fun String.toHomeNavigationPrimaryTabs(): List<HomeScreenTabs> {
    if (isBlank()) return emptyList()

    return sanitizeHomeNavigationPrimaryTabs(
        split(',').mapNotNull { serializedTab ->
            HomeScreenTabs.entries.find { it.name == serializedTab }
        },
    )
}

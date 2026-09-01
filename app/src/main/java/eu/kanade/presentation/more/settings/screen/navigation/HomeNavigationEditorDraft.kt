package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.runtime.saveable.Saver
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.navigation.HomeNavigationConfiguration
import mihon.core.common.navigation.defaultHomeNavigationConfiguration
import mihon.core.common.navigation.resolveHomeNavigationConfiguration
import mihon.core.common.resolveHomeScreenTab
import mihon.core.common.toHomeScreenTabs

internal data class HomeNavigationEditorDraft(
    val configuration: HomeNavigationConfiguration,
    val startupTab: HomeScreenTabs,
    val previewTab: HomeScreenTabs,
) {
    companion object {
        val Saver = Saver<HomeNavigationEditorDraft, String>(
            save = { it.serialize() },
            restore = String::toNavigationDraftOrNull,
        )
    }

    fun serialize(): String {
        return listOf(
            configuration.primaryTabs.joinToString(",") { it.name },
            configuration.overflowTabs.joinToString(",") { it.name },
            configuration.hiddenTabs.joinToString(",") { it.name },
            startupTab.name,
            previewTab.name,
        ).joinToString(";")
    }
}

internal fun HomeNavigationEditorDraft.hasPersistedChangesFrom(other: HomeNavigationEditorDraft): Boolean {
    return configuration != other.configuration || startupTab != other.startupTab
}

internal fun defaultHomeNavigationEditorDraft(): HomeNavigationEditorDraft {
    val configuration = defaultHomeNavigationConfiguration()
    return HomeNavigationEditorDraft(
        configuration = configuration,
        startupTab = resolveHomeScreenTab(
            requestedTab = HomeScreenTabs.Library,
            enabledTabs = configuration.enabledTabs.filterNot { it == HomeScreenTabs.Profiles },
            tabOrder = configuration.tabOrder,
        ),
        previewTab = HomeScreenTabs.Library,
    )
}

internal fun String.toNavigationDraftOrNull(): HomeNavigationEditorDraft? {
    if (isBlank()) return null
    val values = split(';')
    if (values.size != 5) return null
    val startupTab = HomeScreenTabs.entries.find { it.name == values[3] } ?: return null
    val previewTab = HomeScreenTabs.entries.find { it.name == values[4] } ?: return null
    val configuration = HomeNavigationConfiguration(
        primaryTabs = values[0].toTabs(),
        overflowTabs = values[1].toTabs(),
        hiddenTabs = values[2].toTabs(),
    )
    if (configuration.tabOrder.toSet() != HomeScreenTabs.entries.toSet()) return null
    return HomeNavigationEditorDraft(
        configuration = configuration,
        startupTab = startupTab,
        previewTab = previewTab,
    )
}

internal fun CustomPreferences.readCommittedNavigationDraft(): HomeNavigationEditorDraft {
    val configuration = resolveHomeNavigationConfiguration(
        enabledTabs = homeScreenTabs.get().toHomeScreenTabs(),
        tabOrder = homeScreenTabOrder.get(),
        primaryTabs = homeScreenPrimaryTabs.get(),
    )
    val startupTab = resolveHomeScreenTab(
        requestedTab = homeScreenStartupTab.get(),
        enabledTabs = configuration.enabledTabs.filterNot { it == HomeScreenTabs.Profiles },
        tabOrder = configuration.tabOrder,
    )
    return HomeNavigationEditorDraft(
        configuration = configuration,
        startupTab = startupTab,
        previewTab = startupTab,
    )
}

private fun String.toTabs(): List<HomeScreenTabs> {
    if (isEmpty()) return emptyList()
    return split(',').mapNotNull { name -> HomeScreenTabs.entries.find { it.name == name } }
}

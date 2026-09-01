package eu.kanade.presentation.more.settings.screen.navigation

import io.kotest.matchers.shouldBe
import mihon.core.common.HomeScreenTabs
import mihon.core.common.navigation.HomeNavigationConfiguration
import org.junit.jupiter.api.Test

class HomeNavigationEditorDraftTest {

    @Test
    fun `staged navigation survives serialization without changing placement`() {
        val draft = HomeNavigationEditorDraft(
            configuration = HomeNavigationConfiguration(
                primaryTabs = listOf(
                    HomeScreenTabs.Library,
                    HomeScreenTabs.More,
                    HomeScreenTabs.History,
                    HomeScreenTabs.Updates,
                ),
                overflowTabs = listOf(HomeScreenTabs.Browse),
                hiddenTabs = listOf(
                    HomeScreenTabs.Profiles,
                    HomeScreenTabs.Translator,
                    HomeScreenTabs.Statistics,
                ),
            ),
            startupTab = HomeScreenTabs.History,
            previewTab = HomeScreenTabs.Browse,
        )

        draft.serialize().toNavigationDraftOrNull() shouldBe draft
    }

    @Test
    fun `invalid staged navigation is ignored`() {
        "Library;More;;Library;Library".toNavigationDraftOrNull() shouldBe null
        "not a navigation draft".toNavigationDraftOrNull() shouldBe null
    }

    @Test
    fun `preview selection does not make navigation settings dirty`() {
        val committed = HomeNavigationEditorDraft(
            configuration = HomeNavigationConfiguration(
                primaryTabs = listOf(HomeScreenTabs.Library),
                overflowTabs = listOf(HomeScreenTabs.More),
                hiddenTabs = HomeScreenTabs.entries.filterNot {
                    it == HomeScreenTabs.Library || it == HomeScreenTabs.More
                },
            ),
            startupTab = HomeScreenTabs.Library,
            previewTab = HomeScreenTabs.Library,
        )

        committed.copy(previewTab = HomeScreenTabs.More).hasPersistedChangesFrom(committed) shouldBe false
        committed.copy(startupTab = HomeScreenTabs.More).hasPersistedChangesFrom(committed) shouldBe true
        committed.copy(
            configuration = committed.configuration.copy(
                primaryTabs = listOf(HomeScreenTabs.More),
                overflowTabs = listOf(HomeScreenTabs.Library),
            ),
        ).hasPersistedChangesFrom(committed) shouldBe true
    }
}

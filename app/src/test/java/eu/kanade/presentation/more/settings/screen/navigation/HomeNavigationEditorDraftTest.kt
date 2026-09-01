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
                hiddenTabs = listOf(HomeScreenTabs.Profiles),
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
}

package app.katari.sync.ui.choice

import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.setting.scope.DeviceBound
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.SettingsArea
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingSyncStateTest {

    @Test
    fun `a setting synced by default switches out of its area and back`() {
        settingSyncState(ANIMATION_SPEED, ChoiceSet()) shouldBe
            SettingSyncState.Switch(ANIMATION_SPEED, SettingsArea.READERS, on = true, syncedByDefault = true)
        settingSyncState(ANIMATION_SPEED, ChoiceSet(settingsOff = setOf(ANIMATION_SPEED))) shouldBe
            SettingSyncState.Switch(ANIMATION_SPEED, SettingsArea.READERS, on = false, syncedByDefault = true)
    }

    @Test
    fun `a setting that stays on each device by default switches on when chosen`() {
        settingSyncState(COVER_SIZE, ChoiceSet()) shouldBe
            SettingSyncState.Switch(COVER_SIZE, SettingsArea.LIBRARY, on = false, syncedByDefault = false)
        settingSyncState(COVER_SIZE, ChoiceSet(settingsOn = setOf(COVER_SIZE))) shouldBe
            SettingSyncState.Switch(COVER_SIZE, SettingsArea.LIBRARY, on = true, syncedByDefault = false)
    }

    @Test
    fun `a setting set from a menu switches with the menu`() {
        settingSyncState(LIBRARY_SORT, ChoiceSet(menusOff = setOf(MenuSettings.LIBRARY_SORT))) shouldBe
            SettingSyncState.Menu(SettingsArea.LIBRARY, MenuSettings.LIBRARY_SORT, on = false)
    }

    @Test
    fun `a menu's own mark switches the menu while its area syncs, and offers the area otherwise`() {
        menuSyncState(MenuSettings.SOURCES, ChoiceSet()) shouldBe
            SettingSyncState.Menu(SettingsArea.BROWSE, MenuSettings.SOURCES, on = true)
        menuSyncState(MenuSettings.SOURCES, ChoiceSet(areasOff = setOf(SettingsArea.BROWSE))) shouldBe
            SettingSyncState.AreaOff(SettingsArea.BROWSE)
    }

    @Test
    fun `an area that doesn't sync offers the area, whatever the setting's own choice`() {
        val choices = ChoiceSet(areasOff = setOf(SettingsArea.READERS), settingsOff = setOf(ANIMATION_SPEED))
        settingSyncState(ANIMATION_SPEED, choices) shouldBe SettingSyncState.AreaOff(SettingsArea.READERS)
    }

    @Test
    fun `a setting that goes with data follows the data`() {
        settingSyncState(DAILY_GOAL, ChoiceSet(dataOff = setOf(DataGroup.STATISTICS))) shouldBe
            SettingSyncState.WithData(DataGroup.STATISTICS, synced = false)
        settingSyncState(DEFAULT_CATEGORY, ChoiceSet(dataOff = setOf(DataGroup.CATEGORIES))) shouldBe
            SettingSyncState.WithData(DataGroup.CATEGORIES, synced = false)
    }

    @Test
    fun `a setting made for this device says why it stays, and syncing's own options show nothing`() {
        settingSyncState(EINK_FLASH, ChoiceSet()) shouldBe
            SettingSyncState.DeviceOnly(SettingsArea.READERS, DeviceBound.HARDWARE)
        settingSyncState("sync_in_background", ChoiceSet()) shouldBe null
    }

    private companion object {
        const val ANIMATION_SPEED = "pref_double_tap_anim_speed"
        const val COVER_SIZE = "pref_library_cover_size"
        const val LIBRARY_SORT = "library_sorting_mode"
        const val DAILY_GOAL = "statistics_daily_goal_minutes"
        const val DEFAULT_CATEGORY = "default_category"
        const val EINK_FLASH = "pref_reader_flash"
    }
}

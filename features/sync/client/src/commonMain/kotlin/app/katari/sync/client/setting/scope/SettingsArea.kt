package app.katari.sync.client.setting.scope

import app.katari.sync.client.part.DataGroup

/**
 * The areas of Settings whose settings a device can choose to sync, in Settings' order. [followsData] marks settings
 * that go with a data group instead of an area of their own, such as the statistics' daily goal with the statistics.
 */
enum class SettingsArea(val followsData: DataGroup? = null) {
    APPEARANCE,
    LIBRARY,
    LIBRARY_UPDATES,
    READERS,
    PLAYERS,
    TRANSLATION,
    TEXT_TO_SPEECH,
    BROWSE,
    SECURITY,

    /** Each source's own settings, which extensions declare rather than any table here. */
    EXTENSION_SETTINGS,
    STATISTICS(followsData = DataGroup.STATISTICS),
    ;

    companion object {
        /** The areas a device turns on and off by themselves. */
        val switchable: List<SettingsArea> = entries.filter { it.followsData == null }
    }
}

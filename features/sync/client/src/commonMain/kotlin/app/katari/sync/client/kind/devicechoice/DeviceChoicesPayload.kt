package app.katari.sync.client.kind.devicechoice

import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.part.enumsNamed
import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.sync.protocol.device.DeviceId
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType

/** What [device] chose to sync, as it last told the account. */
@Serializable
data class DeviceChoicesPayload(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val device: DeviceId,
    @ProtoNumber(2) val choices: Stamped<PublishedChoices>,
)

/**
 * A device's [ChoiceSet] by names, sorted, so the same choices always encode the same way. Its profiles are left out:
 * whether a device syncs a setting is told apart from the profile the setting is in, as it is on the device itself.
 */
@Serializable
data class PublishedChoices(
    @ProtoNumber(1) val dataOff: List<String> = emptyList(),
    @ProtoNumber(2) val areasOff: List<String> = emptyList(),
    @ProtoNumber(3) val settingsOff: List<String> = emptyList(),
    @ProtoNumber(4) val menusOff: List<String> = emptyList(),
    @ProtoNumber(5) val settingsOn: List<String> = emptyList(),
) {
    fun choiceSet(): ChoiceSet = ChoiceSet(
        dataOff = dataOff.enumsNamed(DataGroup.entries),
        areasOff = areasOff.enumsNamed(SettingsArea.entries),
        settingsOff = settingsOff.toSet(),
        menusOff = menusOff.enumsNamed(MenuSettings.entries),
        settingsOn = settingsOn.toSet(),
    )

    companion object {
        fun of(choices: ChoiceSet) = PublishedChoices(
            dataOff = choices.dataOff.map { it.name }.sorted(),
            areasOff = choices.areasOff.map { it.name }.sorted(),
            settingsOff = choices.settingsOff.sorted(),
            menusOff = choices.menusOff.map { it.name }.sorted(),
            settingsOn = choices.settingsOn.sorted(),
        )
    }
}

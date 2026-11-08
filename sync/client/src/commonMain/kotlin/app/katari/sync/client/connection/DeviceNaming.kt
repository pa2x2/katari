package app.katari.sync.client.connection

import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.protocol.device.DeviceDescription
import dev.zacsweers.metro.Inject

/**
 * How this device presents itself in its account's device list (D005): the name the person gave it here, else the
 * platform's name for it, with its platform and the app's version. The description goes with every pull, so a new
 * name reaches the account with the next sync and needs nothing new of the server.
 */
@Inject
class DeviceNaming(
    private val device: ThisDevice,
    private val preferences: SyncPreferences,
) {
    fun description(): DeviceDescription = DeviceDescription(name(), device.platform, device.appVersion)

    fun name(): String = preferences.deviceName.get().ifBlank { device.platformName() }

    fun platformName(): String = device.platformName()

    /** Names the device [name]; a blank one gives it the platform's name again. */
    fun rename(name: String) {
        preferences.deviceName.set(name.trim().take(DeviceDescription.MAX_FIELD_LENGTH))
    }

    /** Numbers the device's name when another device of the account, whose names are [others], already has it. */
    fun setApart(others: Collection<String>) {
        val name = name()
        val apart = nameApart(name, others)
        if (apart != name) preferences.deviceName.set(apart)
    }
}

/**
 * [name], or the first of "name 2", "name 3" and on that none of [taken] is, ignoring case, since two phones of one
 * model both call themselves by it. The number stays within the server's length limit.
 */
internal fun nameApart(name: String, taken: Collection<String>): String {
    val names = taken.mapTo(HashSet()) { it.trim().lowercase() }
    if (name.lowercase() !in names) return name
    return generateSequence(2) { it + 1 }
        .map { number -> "${name.take(DeviceDescription.MAX_FIELD_LENGTH - " $number".length).trimEnd()} $number" }
        .first { it.lowercase() !in names }
}

package app.katari.sync.client.setting.scope

/** Whether a preference follows the person to every device they sync, or stays with the device it was set on. */
enum class PreferenceScope {
    /** Taste, organisation, language, content rules and reading style: synced, per profile or account-wide. */
    PERSON,

    /**
     * Fitted to the device, as screen sizes and layouts are, so it stays on the device unless the person chooses to
     * sync it there ([app.katari.sync.client.part.ChoiceSet.settingsOn]).
     */
    DEVICE_BY_DEFAULT,

    /**
     * Only works on the device it was set on ([DeviceBound] says why), and every key no table marks: never synced.
     */
    DEVICE,
}

/**
 * How a key or key family syncs. [area] is the Settings area it belongs to: the one it syncs with, or, for a key that
 * only works on the device, the one whose page lists it as staying there; null for such a key no area lists. [bound]
 * says why a key only works on the device.
 */
class PreferenceMark internal constructor(
    val scope: PreferenceScope,
    val area: SettingsArea?,
    val bound: DeviceBound?,
)

package app.katari.sync.client.setting.scope

/** Whether a preference follows the person to every device they sync, or stays with the device it was set on. */
enum class PreferenceScope {
    /** Taste, organisation, language, content rules and reading style: synced, per profile or account-wide. */
    PERSON,

    /**
     * Screen size and layout, hardware, storage, network, power, notifications, installation, update channel and
     * telemetry, and every key no table marks: never synced.
     */
    DEVICE,
}

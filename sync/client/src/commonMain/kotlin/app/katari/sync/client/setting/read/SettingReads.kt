package app.katari.sync.client.setting.read

/** The values preference stores hold now, as each store holds them, null for a preference that isn't set. */
interface SettingReads {
    fun profileValue(profileId: Long, key: String): Any?

    fun sharedValue(key: String): Any?

    /** [profileId]'s setting [key] of the source with its own ID [sourceId]. */
    fun sourceValue(profileId: Long, sourceId: Long, key: String): Any?

    /** Whether [key] belongs to a profile, wherever it was written. */
    fun isProfileKey(key: String): Boolean
}

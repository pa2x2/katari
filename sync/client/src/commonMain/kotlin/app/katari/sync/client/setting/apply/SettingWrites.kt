package app.katari.sync.client.setting.apply

/**
 * Writes preference values into the stores the app's preference classes and the sources read, as [SettingReads]
 * reads them. A null value removes the preference, so it reads as its default.
 *
 * Sync writes them while capture listens. An applied value is stored as its record before it is written here, so the
 * edit the store reports back finds the value already stored and changes nothing.
 */
interface SettingWrites {
    fun writeProfileValue(profileId: Long, key: String, value: Any?)

    fun writeSharedValue(key: String, value: Any?)

    /** [profileId]'s setting [key] of the source with its own ID [sourceId]. */
    fun writeSourceValue(profileId: Long, sourceId: Long, key: String, value: Any?)

    /** Removes every preference and source setting of [profileId], whose profile sync deleted. */
    fun deleteProfile(profileId: Long)
}

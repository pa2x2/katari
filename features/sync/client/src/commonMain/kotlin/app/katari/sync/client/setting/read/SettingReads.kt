package app.katari.sync.client.setting.read

/** The values preference stores hold now, as each store holds them, null for a preference that isn't set. */
interface SettingReads {
    fun profileValue(profileId: Long, key: String): Any?

    fun sharedValue(key: String): Any?

    /** [profileId]'s setting [key] of the source with its own ID [sourceId]. */
    fun sourceValue(profileId: Long, sourceId: Long, key: String): Any?

    /** The keys [profileId]'s store holds a value for. */
    fun profileKeys(profileId: Long): Set<String>

    fun sharedKeys(): Set<String>

    /** The keys [profileId]'s settings hold a value for, by the source's own ID. */
    fun sourceKeys(profileId: Long): Map<Long, Set<String>>

    /** Whether [key] belongs to a profile, wherever it was written. */
    fun isProfileKey(key: String): Boolean
}

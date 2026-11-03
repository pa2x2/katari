package mihon.feature.profiles.core

/**
 * What profiles need from the installed extensions. Each extension keeps its settings per profile itself, outside the
 * profile's preference stores, so a profile's deletion removes them separately.
 */
interface ProfileExtensions {
    /** The sources of every installed extension, which a new profile starts with hidden. */
    suspend fun installedSourceIds(): Set<Long>

    /** Removes the extension settings of a profile that is being deleted, or whose ID is being reused. */
    fun deleteSettings(profileId: Long)
}

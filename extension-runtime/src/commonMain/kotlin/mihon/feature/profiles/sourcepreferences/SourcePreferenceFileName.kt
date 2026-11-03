package mihon.feature.profiles.sourcepreferences

private const val PREFIX = "source_"
private val PROFILE_FILE = Regex("""source_-?\d+_(-?\d+)""")

/** The file holding the settings of the source with its own ID [sourceId] for [profileId]. */
fun sourcePreferenceFileName(
    profileId: Long,
    sourceId: Long,
): String = "${profileSourcePreferenceFilePrefix(profileId)}$sourceId"

/** What the names of all of [profileId]'s source settings files start with. */
fun profileSourcePreferenceFilePrefix(profileId: Long): String = "$PREFIX${profileId}_"

/**
 * The file name extensions open for their settings themselves, with no profile: `source_<id>` in source-api's
 * `preferenceKey()` and Keiyoushi's `getPreferences()`. Before profiles owned source settings it was the file itself.
 */
fun extensionPreferenceFileName(sourceId: Long): String = "$PREFIX$sourceId"

/**
 * The source ID in [name] if it is [extensionPreferenceFileName], else null. Katari's own `source_<profile>_<id>` files
 * have a second number, so they never match.
 */
fun extensionPreferenceSourceId(name: String): Long? {
    if (!name.startsWith(PREFIX)) return null
    return name.substring(PREFIX.length).toLongOrNull()
}

/**
 * The source ID in the name a backup recorded a source's settings under, for backups that don't record the source:
 * Mihon's [extensionPreferenceFileName] or Katari's [sourcePreferenceFileName]. Null for any other name.
 */
fun backedUpSourcePreferenceSourceId(fileName: String): Long? {
    return extensionPreferenceSourceId(fileName)
        ?: PROFILE_FILE.matchEntire(fileName)?.groupValues?.get(1)?.toLongOrNull()
}

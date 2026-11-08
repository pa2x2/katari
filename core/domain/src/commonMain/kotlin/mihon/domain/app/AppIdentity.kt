package mihon.domain.app

/**
 * The running build of Katari as it names itself, such as to extensions and in backup file names. Each platform's graph
 * provides it from that platform's build.
 */
data class AppIdentity(
    /** The installed application's ID, such as `app.katari.foss` on Android, including the build type's suffix. */
    val applicationId: String,
    val versionName: String,
    /** The short hash of the commit the build was made from. */
    val commitSha: String,
    /** The operating system's name as user agents spell it, such as `Android` or `Linux`. */
    val platformName: String,
)

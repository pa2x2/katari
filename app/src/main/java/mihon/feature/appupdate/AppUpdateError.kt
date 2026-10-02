package mihon.feature.appupdate

/** Why a check, download or install failed, in the terms the user is told about it. */
internal sealed interface AppUpdateError {
    data object GithubUnreachable : AppUpdateError

    /** GitHub allows 60 anonymous API calls an hour per IP address. */
    data object GithubRateLimited : AppUpdateError
    data class GithubStatus(val code: Int) : AppUpdateError
    data object GithubUnexpected : AppUpdateError

    data class DownloadFailed(val detail: String?) : AppUpdateError

    /** The download doesn't match GitHub's digest or isn't a readable APK; it is deleted, so a retry starts afresh. */
    data object Damaged : AppUpdateError

    /** Android refuses to install an APK signed with another key over the installed app. */
    data object SignatureMismatch : AppUpdateError

    /** The APK is for another package or isn't a higher versionCode than the installed app. */
    data object NotNewer : AppUpdateError
    data object InsufficientStorage : AppUpdateError
    data object Incompatible : AppUpdateError
    data class InstallFailed(val detail: String?) : AppUpdateError
}

internal class AppUpdateException(val error: AppUpdateError) : Exception(error.toString())

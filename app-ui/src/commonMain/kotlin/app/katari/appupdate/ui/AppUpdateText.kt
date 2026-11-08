package app.katari.appupdate.ui

import androidx.compose.runtime.Composable
import app.katari.appupdate.AppUpdateError
import app.katari.appupdate.AppUpdateStatus
import app.katari.appupdate.check.AvailableUpdate
import eu.kanade.presentation.util.relativeTimeSpan
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.shortFileSize
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun AppUpdateError.message(): String = when (this) {
    AppUpdateError.GithubUnreachable -> stringResource(MR.strings.app_update_error_github_unreachable)
    AppUpdateError.GithubRateLimited -> stringResource(MR.strings.app_update_error_github_rate_limited)
    is AppUpdateError.GithubStatus -> stringResource(MR.strings.app_update_error_github_status, code)
    AppUpdateError.GithubUnexpected -> stringResource(MR.strings.app_update_error_github_unexpected)
    is AppUpdateError.DownloadFailed -> detail?.let { stringResource(MR.strings.app_update_error_download_detail, it) }
        ?: stringResource(MR.strings.app_update_error_download)
    AppUpdateError.Damaged -> stringResource(MR.strings.app_update_error_damaged)
    AppUpdateError.SignatureMismatch -> stringResource(MR.strings.app_update_error_signature)
    AppUpdateError.NotNewer -> stringResource(MR.strings.app_update_error_not_newer)
    AppUpdateError.InsufficientStorage -> stringResource(MR.strings.app_update_error_storage)
    AppUpdateError.Incompatible -> stringResource(MR.strings.app_update_error_incompatible)
    is AppUpdateError.InstallFailed -> detail?.let { stringResource(MR.strings.app_update_error_install_detail, it) }
        ?: stringResource(MR.strings.app_update_error_install)
    is AppUpdateError.NotReplaceable -> stringResource(MR.strings.app_update_error_not_replaceable, path)
}

/**
 * "Version 1.13.0 · 61.6 MB", with "Pre-release" for one and [extra] last, such as the download of what the update
 * brings along.
 */
@Composable
internal fun AvailableUpdate.summary(extra: String?): String = listOfNotNull(
    stringResource(MR.strings.app_update_version, version),
    shortFileSize(asset.size),
    stringResource(MR.strings.app_update_channel_prerelease).takeIf { prerelease },
    extra,
).joinToString(" · ")

@Composable
internal fun downloadingText(progress: Float?): String =
    progress?.let { stringResource(MR.strings.downloading_with_progress, (it * 100).toInt()) }
        ?: stringResource(MR.strings.download_state_downloading)

/** The Check for updates row's subtitle; null when there is nothing to say yet. */
@Composable
internal fun AppUpdateStatus.subtitle(update: AvailableUpdate?, lastCheckedAt: Long): String? = when (this) {
    AppUpdateStatus.Checking -> stringResource(MR.strings.app_update_checking)
    AppUpdateStatus.UpToDate -> stringResource(MR.strings.app_update_up_to_date, relativeTime(lastCheckedAt))
    AppUpdateStatus.Available -> update?.let { stringResource(MR.strings.app_update_version_available, it.version) }
    is AppUpdateStatus.Downloading -> downloadingText(progress)
    AppUpdateStatus.NeedsPermission -> stringResource(MR.strings.app_update_waiting_permission)
    AppUpdateStatus.Installing -> stringResource(MR.strings.app_update_installing)
    AppUpdateStatus.ReadyToRestart -> update?.let { stringResource(MR.strings.app_update_restart_needed, it.version) }
    is AppUpdateStatus.Failed -> error.message()
    AppUpdateStatus.Idle -> lastCheckedAt.takeIf { it > 0 }
        ?.let { stringResource(MR.strings.app_update_last_checked, relativeTime(it)) }
}

@Composable
private fun relativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    // The relative time would say "0 minutes ago" right after a check.
    if (now - epochMillis < 1.minutes.inWholeMilliseconds) return stringResource(MR.strings.app_update_just_now)
    return relativeTimeSpan(epochMillis, now)
}

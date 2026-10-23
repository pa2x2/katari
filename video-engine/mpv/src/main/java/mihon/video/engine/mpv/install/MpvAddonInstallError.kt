package mihon.video.engine.mpv.install

import android.content.Context
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** Why looking up, downloading or installing the add-on failed, in the terms the user is told about it. */
sealed interface MpvAddonInstallError {
    /** GitHub couldn't be reached, which is usually being offline. */
    data object Unreachable : MpvAddonInstallError
    data object RateLimited : MpvAddonInstallError
    data class GithubStatus(val code: Int) : MpvAddonInstallError
    data object GithubUnexpected : MpvAddonInstallError

    /** No published add-on fits this Katari's glue level, version and ABI. */
    data object NoRelease : MpvAddonInstallError
    data class DownloadFailed(val detail: String?) : MpvAddonInstallError
    data object Damaged : MpvAddonInstallError

    /** The add-on isn't signed with Katari's key, as with GitHub's add-on on a locally built Katari. */
    data object SignatureMismatch : MpvAddonInstallError
    data object InsufficientStorage : MpvAddonInstallError
    data object Incompatible : MpvAddonInstallError
    data class InstallFailed(val detail: String?) : MpvAddonInstallError
}

fun MpvAddonInstallError.describe(context: Context): String = when (this) {
    MpvAddonInstallError.Unreachable -> context.stringResource(MR.strings.app_update_error_github_unreachable)
    MpvAddonInstallError.RateLimited -> context.stringResource(MR.strings.app_update_error_github_rate_limited)
    is MpvAddonInstallError.GithubStatus -> context.stringResource(MR.strings.app_update_error_github_status, code)
    MpvAddonInstallError.GithubUnexpected -> context.stringResource(MR.strings.app_update_error_github_unexpected)
    MpvAddonInstallError.NoRelease -> context.stringResource(MR.strings.mpv_addon_error_no_release)
    is MpvAddonInstallError.DownloadFailed ->
        detail
            ?.let { context.stringResource(MR.strings.app_update_error_download_detail, it) }
            ?: context.stringResource(MR.strings.app_update_error_download)
    MpvAddonInstallError.Damaged -> context.stringResource(MR.strings.app_update_error_damaged)
    MpvAddonInstallError.SignatureMismatch -> context.stringResource(MR.strings.mpv_addon_error_signature)
    MpvAddonInstallError.InsufficientStorage -> context.stringResource(MR.strings.mpv_addon_error_storage)
    MpvAddonInstallError.Incompatible -> context.stringResource(MR.strings.mpv_addon_error_incompatible)
    is MpvAddonInstallError.InstallFailed ->
        detail
            ?.let { context.stringResource(MR.strings.mpv_addon_error_install_detail, it) }
            ?: context.stringResource(MR.strings.mpv_addon_error_install)
}

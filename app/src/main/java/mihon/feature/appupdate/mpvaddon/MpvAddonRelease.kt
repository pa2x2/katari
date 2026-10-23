package mihon.feature.appupdate.mpvaddon

import mihon.feature.appupdate.check.UpdateApk

/** A published mpv add-on, as offered for install or update. */
internal data class MpvAddonRelease(
    /** The libmpv release, such as `2026-11-02-1`. */
    val version: String,
    val versionCode: Long,
    val glue: Int,
    /** The add-on APK for Katari's process ABI. */
    val apk: UpdateApk,
    /** "mpv 0.41.0, FFmpeg 9.0.2", or null when the release doesn't list them. */
    val components: String?,
)

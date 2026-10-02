package mihon.feature.appupdate.check

/** The newest release this build can update to, with the notes of every release since the installed version. */
internal data class AvailableUpdate(
    /** Version without the tag's `v`, e.g. `1.13.0` or `1.13.0-beta1`. */
    val version: String,
    val prerelease: Boolean,
    /** Newest first, starting with this update's own notes. Releases without notes are left out. */
    val notes: List<VersionNotes>,
    val pageUrl: String,
    val apk: UpdateApk,
)

internal data class VersionNotes(
    val version: String,
    /** Markdown. */
    val notes: String,
)

internal data class UpdateApk(
    val name: String,
    val url: String,
    /** In bytes, as GitHub reports it. */
    val size: Long,
    /** Lowercase hex, or null for an asset GitHub has no digest for. */
    val sha256: String?,
)

package mihon.feature.appupdate.check

import mihon.feature.appupdate.mpvaddon.MpvAddonRelease

/** The newest release this build can update to, with the notes of every release since the installed version. */
internal data class AvailableUpdate(
    /** Version without the tag's `v`, e.g. `1.13.0` or `1.13.0-beta1`. */
    val version: String,
    val prerelease: Boolean,
    /** Newest first, starting with this update's own notes. Releases without notes are left out. */
    val notes: List<VersionNotes>,
    val pageUrl: String,
    val apk: UpdateAsset,
    /** Where the release says which mpv add-on it needs; null for releases from before the add-on. */
    val mpvRequirementUrl: String? = null,
    /** The mpv add-on to install first, because the installed one won't run with this version. */
    val addon: MpvAddonRelease? = null,
)

/** The Android app's releases are tagged `v<version>`. */
internal const val ANDROID_TAG_PREFIX = "v"

internal fun ReleaseUpdate.toAvailableUpdate(): AvailableUpdate = AvailableUpdate(
    version = version,
    prerelease = prerelease,
    notes = notes,
    pageUrl = release.pageUrl,
    apk = asset,
    mpvRequirementUrl = release.assets.find { it.name == mpvRequirementAssetName(release.tagName) }?.downloadUrl,
)

/** The mpv add-on a release needs, as the release workflow publishes it. */
private fun mpvRequirementAssetName(tag: String) = "katari-$tag-mpv.json"

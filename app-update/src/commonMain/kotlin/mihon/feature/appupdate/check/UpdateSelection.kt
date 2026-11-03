package mihon.feature.appupdate.check

/**
 * The newest published release of one app on [channel] that is newer than [installedVersion] and carries a file
 * [selectAsset] picks for this build, or null when there is none.
 *
 * The Android and desktop apps release from one repository, so only tags starting with [tagPrefix] belong to the app;
 * the version is the rest of the tag. Drafts never count (GitHub also hides them from anonymous calls). On the stable
 * channel a release is skipped when GitHub flags it as a pre-release or its tag carries a pre-release version, since the
 * release workflow can publish either.
 */
fun selectUpdate(
    releases: List<GithubRelease>,
    tagPrefix: String,
    channel: AppUpdateChannel,
    installedVersion: String,
    selectAsset: (List<GithubAsset>) -> GithubAsset?,
): ReleaseUpdate? {
    val installed = ReleaseVersion.parse(installedVersion) ?: return null
    val newer = releases
        .mapNotNull { release ->
            if (release.draft || !release.tagName.startsWith(tagPrefix)) return@mapNotNull null
            val versionName = release.tagName.removePrefix(tagPrefix)
            val version = ReleaseVersion.parse(versionName) ?: return@mapNotNull null
            val prerelease = release.prerelease || version.isPrerelease
            if (prerelease && channel == AppUpdateChannel.STABLE) return@mapNotNull null
            if (version <= installed) return@mapNotNull null
            Candidate(release, versionName, version, prerelease)
        }
        .sortedByDescending { it.version }

    val (target, asset) = newer.firstNotNullOfOrNull { candidate ->
        selectAsset(candidate.release.assets)?.let { candidate to it }
    } ?: return null

    return ReleaseUpdate(
        release = target.release,
        version = target.versionName,
        prerelease = target.prerelease,
        asset = asset.toUpdateAsset(),
        notes = newer
            .filter { it.version <= target.version }
            .mapNotNull { candidate ->
                cleanReleaseNotes(candidate.release.body.orEmpty())
                    .takeIf { it.isNotEmpty() }
                    ?.let { VersionNotes(candidate.versionName, it) }
            },
    )
}

/** The release an app can update to, with the notes of every release since the installed version. */
data class ReleaseUpdate(
    val release: GithubRelease,
    /** The tag without its prefix, e.g. `1.13.0` or `1.13.0-beta1`. */
    val version: String,
    val prerelease: Boolean,
    val asset: UpdateAsset,
    /** Newest first, starting with this update's own notes. Releases without notes are left out. */
    val notes: List<VersionNotes>,
)

data class VersionNotes(
    val version: String,
    /** Markdown. */
    val notes: String,
)

private data class Candidate(
    val release: GithubRelease,
    val versionName: String,
    val version: ReleaseVersion,
    val prerelease: Boolean,
)

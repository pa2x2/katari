package mihon.feature.appupdate.check

/**
 * The newest published release on [channel] that is newer than [installedVersion] and has an APK for this build and
 * device, or null when there is none.
 *
 * Only `v*` tags count; drafts never do (GitHub also hides them from anonymous calls). On the stable channel a release
 * is skipped when GitHub flags it as a pre-release or its tag carries a pre-release version, since the release
 * workflow can publish either.
 */
internal fun selectUpdate(
    releases: List<GithubRelease>,
    channel: AppUpdateChannel,
    installedVersion: String,
    isFoss: Boolean,
    primaryAbi: String,
): AvailableUpdate? {
    val installed = ReleaseVersion.parse(installedVersion) ?: return null
    val newer = releases
        .mapNotNull { release ->
            if (release.draft || !release.tagName.startsWith("v")) return@mapNotNull null
            val version = ReleaseVersion.parse(release.tagName) ?: return@mapNotNull null
            val prerelease = release.prerelease || version.isPrerelease
            if (prerelease && channel == AppUpdateChannel.STABLE) return@mapNotNull null
            if (version <= installed) return@mapNotNull null
            Candidate(release, version, prerelease)
        }
        .sortedByDescending { it.version }

    val (target, apk) = newer.firstNotNullOfOrNull { candidate ->
        selectReleaseApk(candidate.release.assets, isFoss, primaryAbi)?.let { candidate to it }
    } ?: return null

    return AvailableUpdate(
        version = target.release.tagName.removePrefix("v"),
        prerelease = target.prerelease,
        notes = newer
            .filter { it.version <= target.version }
            .mapNotNull { candidate ->
                cleanReleaseNotes(candidate.release.body.orEmpty())
                    .takeIf { it.isNotEmpty() }
                    ?.let { VersionNotes(candidate.release.tagName.removePrefix("v"), it) }
            },
        pageUrl = target.release.pageUrl,
        apk = UpdateApk(
            name = apk.name,
            url = apk.downloadUrl,
            size = apk.size,
            sha256 = apk.digest?.let(SHA256_DIGEST::matchEntire)?.groupValues?.get(1)?.lowercase(),
        ),
    )
}

private data class Candidate(
    val release: GithubRelease,
    val version: ReleaseVersion,
    val prerelease: Boolean,
)

private val SHA256_DIGEST = Regex("""sha256:([0-9a-fA-F]{64})""")

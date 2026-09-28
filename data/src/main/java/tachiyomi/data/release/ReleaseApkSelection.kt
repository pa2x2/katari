package tachiyomi.data.release

/**
 * Picks the release APK that installs over this build: the one for its distribution and the device's primary ABI,
 * else that distribution's universal APK.
 *
 * Release asset names, as the release workflow publishes them:
 * - `katari-<tag>.apk` and `katari-<abi>-<tag>.apk` for the standard build;
 * - `katari-<tag>-foss.apk` and `katari_foss_<abi>-<tag>.apk` for the FOSS build. Per-ABI FOSS names contain neither
 *   `-foss` nor `-<abi>`, which is how earlier app versions recognise assets, so those versions keep choosing the
 *   universal FOSS APK and the standard per-ABI APKs.
 */
internal fun selectReleaseApk(assets: List<GitHubAsset>, isFoss: Boolean, primaryAbi: String): GitHubAsset? {
    val (fossAssets, standardAssets) = assets.partition { it.isFoss() }
    return if (isFoss) {
        fossAssets.find { "$FOSS_ABI_MARKER$primaryAbi-" in it.name }
            ?: fossAssets.find { it.name.endsWith(FOSS_UNIVERSAL_SUFFIX) }
    } else {
        standardAssets.find { "-$primaryAbi-" in it.name }
            ?: standardAssets.find { asset -> ABIS.none { "-$it-" in asset.name } }
    }
}

private fun GitHubAsset.isFoss() = name.endsWith(FOSS_UNIVERSAL_SUFFIX) || FOSS_ABI_MARKER in name

private const val FOSS_UNIVERSAL_SUFFIX = "-foss.apk"
private const val FOSS_ABI_MARKER = "_foss_"
private val ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

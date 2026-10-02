package mihon.feature.appupdate.check

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The slice of GitHub's release JSON the updater reads. */
@Serializable
internal data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val body: String? = null,
    @SerialName("html_url")
    val pageUrl: String,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
internal data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val downloadUrl: String,
    val size: Long = 0,
    /** `sha256:<hex>` for assets uploaded since GitHub started computing digests; absent on older ones. */
    val digest: String? = null,
)

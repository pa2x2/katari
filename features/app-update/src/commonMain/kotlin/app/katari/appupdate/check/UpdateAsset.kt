package app.katari.appupdate.check

/** A file of a release, as the updater downloads and checks it. */
data class UpdateAsset(
    val name: String,
    val url: String,
    /** In bytes, as GitHub reports it. */
    val size: Long,
    /** Lowercase hex, or null for an asset GitHub has no digest for. */
    val sha256: String?,
)

fun GithubAsset.toUpdateAsset(): UpdateAsset = UpdateAsset(name = name, url = downloadUrl, size = size, sha256 = sha256)

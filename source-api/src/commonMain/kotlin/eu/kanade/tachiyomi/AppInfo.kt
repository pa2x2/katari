package eu.kanade.tachiyomi

/**
 * Shared runtime surface for extensions.
 */
@Suppress("UNUSED")
object AppInfo {
    fun getVersionCode(): Int = readAppBuildInt("VERSION_CODE") ?: 0

    fun getVersionName(): String = readAppBuildString("VERSION_NAME") ?: "0"

    fun getSupportedImageMimeTypes(): List<String> = listOf(
        "image/avif",
        "image/gif",
        "image/heif",
        "image/jpeg",
        "image/jxl",
        "image/png",
        "image/webp",
    )

    private fun readAppBuildInt(fieldName: String): Int? {
        return (readAppBuildField(fieldName) as? Number)?.toInt()
    }

    private fun readAppBuildString(fieldName: String): String? {
        return readAppBuildField(fieldName) as? String
    }

    private fun readAppBuildField(fieldName: String): Any? {
        return runCatching {
            Class.forName("eu.kanade.tachiyomi.BuildConfig")
                .getField(fieldName)
                .get(null)
        }.getOrNull()
    }
}

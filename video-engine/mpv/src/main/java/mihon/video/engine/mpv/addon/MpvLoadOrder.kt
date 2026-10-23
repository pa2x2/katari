package mihon.video.engine.mpv.addon

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * A libmpv release's `load-order.json` (format 2): which add-on versionCode and JNI glue level the release has, and in
 * which order its native libraries load on each ABI, dependencies first. The same file is in the libmpv AAR, in the
 * add-on APK and next to the release.
 */
@Serializable
data class MpvLoadOrder(
    val format: Int,
    /** The release, such as `2026-10-23-3`; for display only, compare [versionCode]. */
    val version: String,
    val versionCode: Long,
    val glue: Int,
    /** File names, which are also the sonames, per ABI. */
    val loadOrder: Map<String, List<String>>,
) {
    companion object {
        const val ASSET = "libmpv/load-order.json"

        private const val FORMAT = 2
        private val json = Json { ignoreUnknownKeys = true }

        /** Null for anything but a well-formed format 2 file. */
        fun parse(text: String): MpvLoadOrder? = try {
            json.decodeFromString<MpvLoadOrder>(text).takeIf { it.format == FORMAT }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

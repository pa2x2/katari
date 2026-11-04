package mihon.video.engine.mpv.addon

import android.content.Context
import `is`.xyz.mpv.MPVLib
import mihon.video.engine.mpv.loading.processAbi

/**
 * What this Katari needs of the mpv add-on: the JNI glue level of the `MPVLib` it was built with, at least the
 * add-on versionCode of the libmpv release that `MPVLib` came from, and libraries for the process [abi].
 */
data class MpvAddonRequirement(
    val glue: Int,
    val minVersionCode: Long,
    val abi: String,
) {
    fun acceptsGlue(glue: Int?): Boolean = glue == this.glue

    fun acceptsVersion(versionCode: Long): Boolean = versionCode >= minVersionCode

    companion object {
        /** Read from Katari's own copy of the AAR's `load-order.json`, whose glue level must match `MPVLib`'s. */
        fun of(context: Context): MpvAddonRequirement {
            val own = context.assets.open(MpvLoadOrder.ASSET).bufferedReader().use { MpvLoadOrder.parse(it.readText()) }
            checkNotNull(own) { "Katari's ${MpvLoadOrder.ASSET} isn't a format 2 load order" }
            check(own.glue == MPVLib.GLUE_LEVEL) {
                "Katari's ${MpvLoadOrder.ASSET} has glue level ${own.glue}, but MPVLib has ${MPVLib.GLUE_LEVEL}"
            }
            return MpvAddonRequirement(
                glue = MPVLib.GLUE_LEVEL,
                minVersionCode = own.versionCode,
                abi = context.applicationInfo.processAbi(),
            )
        }
    }
}

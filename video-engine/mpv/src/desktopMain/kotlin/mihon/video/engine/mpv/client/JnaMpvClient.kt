package mihon.video.engine.mpv.client

import com.sun.jna.Memory
import com.sun.jna.Pointer
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * One mpv instance, [handle], through libmpv's C API. The client API may be called from any thread; a call that fails
 * is logged and otherwise ignored, as the add-on's glue does on Android.
 */
internal class JnaMpvClient(private val mpv: LibMpv, val handle: Pointer) : MpvClient {
    override fun setOption(name: String, value: String) {
        check(mpv.setOptionString(handle, name, value)) { "option $name" }
    }

    override fun command(vararg args: String) {
        check(mpv.command(handle, arrayOf(*args))) { "command ${args.firstOrNull()}" }
    }

    override fun getLong(property: String): Long? =
        read(property, MpvFormatCode.INT64, Long.SIZE_BYTES) { it.getLong(0) }

    override fun getDouble(property: String): Double? =
        read(property, MpvFormatCode.DOUBLE, Double.SIZE_BYTES) { it.getDouble(0) }

    override fun getFlag(property: String): Boolean? =
        read(property, MpvFormatCode.FLAG, Int.SIZE_BYTES) { it.getInt(0) != 0 }

    override fun getString(property: String): String? {
        val value = mpv.getPropertyString(handle, property) ?: return null
        return try {
            value.getString(0, Charsets.UTF_8.name())
        } finally {
            mpv.free(value)
        }
    }

    override fun setDouble(property: String, value: Double) {
        val data = Memory(Double.SIZE_BYTES.toLong()).apply { setDouble(0, value) }
        check(mpv.setProperty(handle, property, MpvFormatCode.DOUBLE, data)) { "property $property" }
    }

    override fun setFlag(property: String, value: Boolean) {
        val data = Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, if (value) 1 else 0) }
        check(mpv.setProperty(handle, property, MpvFormatCode.FLAG, data)) { "property $property" }
    }

    override fun setString(property: String, value: String) {
        check(mpv.setPropertyString(handle, property, value)) { "property $property" }
    }

    override fun observe(property: String, format: MpvFormat) {
        val code = when (format) {
            MpvFormat.String -> MpvFormatCode.STRING
            MpvFormat.Flag -> MpvFormatCode.FLAG
            MpvFormat.Int64 -> MpvFormatCode.INT64
            MpvFormat.Double -> MpvFormatCode.DOUBLE
        }
        check(mpv.observeProperty(handle, 0, property, code)) { "observing $property" }
    }

    private inline fun <T> read(property: String, format: Int, size: Int, value: (Memory) -> T): T? {
        val data = Memory(size.toLong())
        return if (mpv.getProperty(handle, property, format, data) >= 0) value(data) else null
    }

    private inline fun check(result: Int, what: () -> String) {
        if (result < 0) logcat(LogPriority.DEBUG) { "mpv ${what()}: ${mpv.errorString(result)}" }
    }
}

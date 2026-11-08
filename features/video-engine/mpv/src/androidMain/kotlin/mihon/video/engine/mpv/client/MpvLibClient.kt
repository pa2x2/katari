package mihon.video.engine.mpv.client

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvFormat as MpvLibFormat

/**
 * The add-on's mpv through `MPVLib`, the JNI glue of the libmpv AAR: one native instance per process, which
 * [mihon.video.engine.mpv.session.MpvInstance] hands to one session at a time.
 */
internal object MpvLibClient : MpvClient {
    override fun setOption(name: String, value: String) {
        MPVLib.setOptionString(name, value)
    }

    override fun command(vararg args: String) = MPVLib.command(arrayOf(*args))

    override fun getLong(property: String): Long? = MPVLib.getPropertyInt(property)?.toLong()

    override fun getDouble(property: String): Double? = MPVLib.getPropertyDouble(property)

    override fun getFlag(property: String): Boolean? = MPVLib.getPropertyBoolean(property)

    override fun getString(property: String): String? = MPVLib.getPropertyString(property)

    override fun setDouble(property: String, value: Double) = MPVLib.setPropertyDouble(property, value)

    override fun setFlag(property: String, value: Boolean) = MPVLib.setPropertyBoolean(property, value)

    override fun setString(property: String, value: String) = MPVLib.setPropertyString(property, value)

    override fun observe(property: String, format: MpvFormat) {
        MPVLib.observeProperty(
            property,
            when (format) {
                MpvFormat.String -> MpvLibFormat.MPV_FORMAT_STRING
                MpvFormat.Flag -> MpvLibFormat.MPV_FORMAT_FLAG
                MpvFormat.Int64 -> MpvLibFormat.MPV_FORMAT_INT64
                MpvFormat.Double -> MpvLibFormat.MPV_FORMAT_DOUBLE
            },
        )
    }
}

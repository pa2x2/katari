package mihon.video.engine.mpv.client

/**
 * The calls Katari makes into a live mpv instance, as libmpv's client API (`client.h`) offers them. Each platform binds
 * libmpv its own way: Android through the add-on's JNI glue, desktop through JNA. A client is called on its session's
 * mpv thread ([MpvSession.post]), or on the event thread while it hands over an event: the event thread runs only
 * while the instance exists, and the client API may be called from any thread.
 */
interface MpvClient {
    /** Before the instance initialises, any option; after, the options mpv reads at the next load, such as `start`. */
    fun setOption(name: String, value: String)

    fun command(vararg args: String)

    /** Null when the property has no value now, or none of this type. */
    fun getLong(property: String): Long?

    fun getDouble(property: String): Double?

    fun getFlag(property: String): Boolean?

    fun getString(property: String): String?

    fun setDouble(property: String, value: Double)

    fun setFlag(property: String, value: Boolean)

    fun setString(property: String, value: String)

    /** Reports the property's changes to the session's host, as [format]; the instance forgets it when destroyed. */
    fun observe(property: String, format: MpvFormat)
}

/** The value types of `client.h` Katari reads properties as. */
enum class MpvFormat {
    String,
    Flag,
    Int64,
    Double,
}

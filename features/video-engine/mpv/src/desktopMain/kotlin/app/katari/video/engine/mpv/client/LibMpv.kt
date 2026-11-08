package app.katari.video.engine.mpv.client

import app.katari.video.engine.mpv.system.MpvSystem
import app.katari.video.engine.mpv.system.mpvSystem
import com.sun.jna.Callback
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.IOException

/**
 * The part of libmpv's `client.h` and `render.h` Katari calls. Each function is the C one with the `mpv_` prefix and in
 * snake case: [setOptionString] is `mpv_set_option_string`. Strings are UTF-8, as libmpv's are.
 */
internal interface LibMpv : Library {
    fun create(): Pointer?

    fun initialize(handle: Pointer): Int

    fun setOptionString(handle: Pointer, name: String, value: String): Int

    /** [args] is passed NULL-terminated, as JNA passes string arrays. */
    fun command(handle: Pointer, args: Array<String>): Int

    /** Fills [data] with the property's value as [format]; negative when it has none, or none of that format. */
    fun getProperty(handle: Pointer, name: String, format: Int, data: Pointer): Int

    fun setProperty(handle: Pointer, name: String, format: Int, data: Pointer): Int

    fun setPropertyString(handle: Pointer, name: String, value: String): Int

    /** The value, to be handed to [free]; null when the property has none. */
    fun getPropertyString(handle: Pointer, name: String): Pointer?

    fun free(data: Pointer)

    fun observeProperty(handle: Pointer, replyUserdata: Long, name: String, format: Int): Int

    fun requestLogMessages(handle: Pointer, minLevel: String): Int

    /** The next event, owned by mpv until the next call; waits up to [timeout] seconds, forever when negative. */
    fun waitEvent(handle: Pointer, timeout: Double): Pointer

    /** Makes a [waitEvent] waiting on another thread return. */
    fun wakeup(handle: Pointer)

    fun terminateDestroy(handle: Pointer)

    fun errorString(error: Int): String?

    fun renderContextCreate(result: PointerByReference, handle: Pointer, params: Pointer): Int

    fun renderContextSetUpdateCallback(context: Pointer, callback: RenderUpdateCallback?, callbackContext: Pointer?)

    /** The `MPV_RENDER_UPDATE_*` bits of what changed since the last call. */
    fun renderContextUpdate(context: Pointer): Long

    fun renderContextRender(context: Pointer, params: Pointer): Int

    fun renderContextReportSwap(context: Pointer)

    fun renderContextFree(context: Pointer)
}

/** mpv's `mpv_render_update_fn`: a frame or anything else may be due. On an mpv thread, which it must not block. */
internal fun interface RenderUpdateCallback : Callback {
    fun invoke(context: Pointer?)
}

/** `client.h`'s `mpv_format` values. */
internal object MpvFormatCode {
    const val NONE = 0
    const val STRING = 1
    const val FLAG = 3
    const val INT64 = 4
    const val DOUBLE = 5
}

/**
 * libmpv, loaded on first use: the copy the app carries, built for x86_64 Linux, or else the system's, and the process
 * readied for it ([MpvSystem.prepareProcess]). Throws [MpvUnavailableException] when neither loads; that is decided
 * once per run.
 */
internal fun libMpv(): LibMpv = loadedLibMpv.getOrElse { throw MpvUnavailableException(it) }

internal class MpvUnavailableException(cause: Throwable) : Exception("libmpv didn't load", cause)

private val loadedLibMpv: Result<LibMpv> by lazy {
    val failures = mutableListOf<Throwable>()
    val names = try {
        listOf(Native.extractFromResourcePath(LIBRARY_NAME, LibMpv::class.java.classLoader).absolutePath)
    } catch (e: IOException) {
        failures += e
        emptyList()
    } + SYSTEM_LIBRARY_NAMES
    for (name in names) {
        try {
            val library = Native.load(name, LibMpv::class.java, LIBRARY_OPTIONS)
            mpvSystem.prepareProcess()
            return@lazy Result.success(library)
        } catch (e: UnsatisfiedLinkError) {
            failures += e
        }
    }
    Result.failure(failures.reduce { first, next -> first.apply { addSuppressed(next) } })
}

private const val LIBRARY_NAME = "mpv"

// The versioned name first: distributions ship the unversioned one only with libmpv's development files
private val SYSTEM_LIBRARY_NAMES = listOf("libmpv.so.2", LIBRARY_NAME)

private val LIBRARY_OPTIONS = mapOf(
    Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method -> "mpv_" + method.name.toSnakeCase() },
    Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name(),
)

private fun String.toSnakeCase(): String = buildString {
    for (char in this@toSnakeCase) {
        if (char.isUpperCase()) append('_').append(char.lowercaseChar()) else append(char)
    }
}

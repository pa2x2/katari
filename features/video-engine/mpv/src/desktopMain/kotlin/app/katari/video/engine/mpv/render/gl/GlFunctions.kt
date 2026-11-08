package app.katari.video.engine.mpv.render.gl

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Pointer

/**
 * The OpenGL calls made around mpv's rendering, through the function pointers [procAddress] looks up by name, as
 * `eglGetProcAddress` does. Calls go to whichever context is current on the calling thread.
 */
internal class GlFunctions(private val procAddress: (String) -> Pointer?) {
    private val functions = HashMap<String, Function>()

    /** mpv's `get_proc_address`, which it looks up every function it calls with. */
    val mpvProcAddress = MpvProcAddress { _, name -> procAddress(name) }

    /** `mpv_opengl_init_params` with [mpvProcAddress]; the trailing pointer is older APIs' unused `extra_exts`. */
    fun mpvInitParams(): Memory = Memory(INIT_PARAMS_SIZE).apply {
        clear()
        setPointer(0, CallbackReference.getFunctionPointer(mpvProcAddress))
    }

    fun genFramebuffer(): Int = genName("glGenFramebuffers")

    fun genRenderbuffer(): Int = genName("glGenRenderbuffers")

    fun deleteFramebuffer(framebuffer: Int) = call("glDeleteFramebuffers", 1, names(framebuffer))

    fun deleteRenderbuffer(renderbuffer: Int) = call("glDeleteRenderbuffers", 1, names(renderbuffer))

    /** Whether [framebuffer] now draws into an RGBA renderbuffer of [width] by [height]. */
    fun attachRenderbuffer(framebuffer: Int, renderbuffer: Int, width: Int, height: Int): Boolean {
        call("glBindRenderbuffer", GL_RENDERBUFFER, renderbuffer)
        call("glRenderbufferStorage", GL_RENDERBUFFER, GL_RGBA8, width, height)
        call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer)
        call("glFramebufferRenderbuffer", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, renderbuffer)
        return complete()
    }

    /** Reads [framebuffer] into [into], BGRA rows [stride] bytes apart, in the order mpv drew them, top row first. */
    fun readPixels(framebuffer: Int, width: Int, height: Int, into: Pointer, stride: Int) {
        call("glBindFramebuffer", GL_READ_FRAMEBUFFER, framebuffer)
        call("glPixelStorei", GL_PACK_ALIGNMENT, 4)
        call("glPixelStorei", GL_PACK_ROW_LENGTH, stride / BYTES_PER_PIXEL)
        call("glReadPixels", 0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, into)
        call("glPixelStorei", GL_PACK_ROW_LENGTH, 0)
    }

    private fun complete(): Boolean {
        val status = function("glCheckFramebufferStatus").invokeInt(arrayOf(GL_FRAMEBUFFER))
        call("glBindFramebuffer", GL_FRAMEBUFFER, 0)
        return status == GL_FRAMEBUFFER_COMPLETE
    }

    private fun genName(generator: String): Int {
        val name = Memory(Int.SIZE_BYTES.toLong())
        call(generator, 1, name)
        return name.getInt(0)
    }

    private fun names(name: Int) = Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, name) }

    private fun call(name: String, vararg args: Any?) = function(name).invoke(args)

    private fun function(name: String): Function = functions.getOrPut(name) {
        Function.getFunction(procAddress(name) ?: error("OpenGL has no $name"))
    }

    fun interface MpvProcAddress : Callback {
        fun invoke(context: Pointer?, name: String): Pointer?
    }

    private companion object {
        const val INIT_PARAMS_SIZE = 24L
        const val BYTES_PER_PIXEL = 4

        const val GL_RGBA8 = 0x8058
        const val GL_BGRA = 0x80E1
        const val GL_UNSIGNED_BYTE = 0x1401
        const val GL_FRAMEBUFFER = 0x8D40
        const val GL_READ_FRAMEBUFFER = 0x8CA8
        const val GL_RENDERBUFFER = 0x8D41
        const val GL_COLOR_ATTACHMENT0 = 0x8CE0
        const val GL_FRAMEBUFFER_COMPLETE = 0x8CD5
        const val GL_PACK_ALIGNMENT = 0x0D05
        const val GL_PACK_ROW_LENGTH = 0x0D02
    }
}

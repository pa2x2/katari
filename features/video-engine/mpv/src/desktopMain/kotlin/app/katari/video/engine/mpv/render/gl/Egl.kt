package app.katari.video.engine.mpv.render.gl

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * The system's EGL, for an OpenGL context of mpv's own off any window. The soname: the JetBrains Runtime carries ANGLE
 * as `libEGL.so`, which offers OpenGL ES only, and a bare name would find it first.
 */
internal interface Egl : Library {
    fun eglGetPlatformDisplay(platform: Int, nativeDisplay: Pointer?, attributes: Pointer?): Pointer?

    fun eglInitialize(display: Pointer, major: Pointer?, minor: Pointer?): Boolean

    fun eglBindAPI(api: Int): Boolean

    fun eglCreateContext(display: Pointer, config: Pointer?, shareContext: Pointer?, attributes: IntArray): Pointer?

    fun eglMakeCurrent(display: Pointer, draw: Pointer?, read: Pointer?, context: Pointer?): Boolean

    fun eglDestroyContext(display: Pointer, context: Pointer): Boolean

    fun eglGetProcAddress(name: String): Pointer?

    fun eglQueryString(display: Pointer?, name: Int): String?

    fun eglGetError(): Int

    companion object {
        val INSTANCE: Egl by lazy { Native.load("libEGL.so.1", Egl::class.java) }

        const val EGL_PLATFORM_SURFACELESS_MESA = 0x31DD
        const val EGL_OPENGL_API = 0x30A2
        const val EGL_CONTEXT_MAJOR_VERSION = 0x3098
        const val EGL_CONTEXT_MINOR_VERSION = 0x30FB
        const val EGL_CONTEXT_OPENGL_PROFILE_MASK = 0x30FD
        const val EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT = 1
        const val EGL_NONE = 0x3038
        const val EGL_EXTENSIONS = 0x3055
        const val EGL_DEVICE_EXT = 0x322C
    }
}

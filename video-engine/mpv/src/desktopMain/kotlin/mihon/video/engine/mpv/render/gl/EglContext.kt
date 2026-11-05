package mihon.video.engine.mpv.render.gl

import com.sun.jna.Function
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * An OpenGL context of EGL's surfaceless platform, current on the thread that made it, on a GPU only. Where EGL only
 * has Mesa's CPU rasterisers, mpv's own software renderer plays at a fraction of their CPU time and drops fewer frames.
 */
internal class EglContext private constructor(private val display: Pointer, private val context: Pointer) {
    fun destroy() {
        val egl = Egl.INSTANCE
        egl.eglMakeCurrent(display, null, null, null)
        egl.eglDestroyContext(display, context)
        // The display stays initialised: EGL shares it with every other user of the platform in the process.
    }

    companion object {
        fun create(): EglContext? {
            val egl = try {
                Egl.INSTANCE
            } catch (e: UnsatisfiedLinkError) {
                logcat(LogPriority.INFO, e) { "No EGL for mpv's OpenGL renderer" }
                return null
            }
            val display = egl.eglGetPlatformDisplay(Egl.EGL_PLATFORM_SURFACELESS_MESA, null, null) ?: return null
            if (!egl.eglInitialize(display, null, null) || !egl.eglBindAPI(Egl.EGL_OPENGL_API)) {
                logcat(LogPriority.INFO) { "EGL has no OpenGL without a window: 0x${egl.eglGetError().toString(16)}" }
                return null
            }
            if (egl.rendersOnCpu(display)) {
                logcat(LogPriority.INFO) { "EGL has no GPU to render mpv's frames on" }
                return null
            }
            val attributes = intArrayOf(
                Egl.EGL_CONTEXT_MAJOR_VERSION,
                3,
                Egl.EGL_CONTEXT_MINOR_VERSION,
                3,
                Egl.EGL_CONTEXT_OPENGL_PROFILE_MASK,
                Egl.EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
                Egl.EGL_NONE,
            )
            val context = egl.eglCreateContext(display, null, null, attributes) ?: run {
                logcat(LogPriority.INFO) { "EGL made no OpenGL context: 0x${egl.eglGetError().toString(16)}" }
                return null
            }
            if (!egl.eglMakeCurrent(display, null, null, context)) {
                egl.eglDestroyContext(display, context)
                return null
            }
            return EglContext(display, context)
        }

        /** Whether [display] is Mesa's, rendering on the CPU, as its device's `EGL_MESA_device_software` tells. */
        private fun Egl.rendersOnCpu(display: Pointer): Boolean {
            val clientExtensions = eglQueryString(null, Egl.EGL_EXTENSIONS).orEmpty().split(' ')
            if ("EGL_EXT_device_query" !in clientExtensions && "EGL_EXT_device_base" !in clientExtensions) return false
            val queryDisplay = eglGetProcAddress("eglQueryDisplayAttribEXT") ?: return false
            val queryDevice = eglGetProcAddress("eglQueryDeviceStringEXT") ?: return false
            val device = Memory(Native.POINTER_SIZE.toLong())
            if (Function.getFunction(queryDisplay).invokeInt(arrayOf(display, Egl.EGL_DEVICE_EXT, device)) == 0) {
                return false
            }
            val deviceExtensions = Function.getFunction(queryDevice)
                .invokeString(arrayOf(device.getPointer(0), Egl.EGL_EXTENSIONS), false)
            return "EGL_MESA_device_software" in deviceExtensions.orEmpty().split(' ')
        }
    }
}

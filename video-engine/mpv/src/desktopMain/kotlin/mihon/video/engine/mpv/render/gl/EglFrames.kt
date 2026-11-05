package mihon.video.engine.mpv.render.gl

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import logcat.LogPriority
import mihon.video.engine.mpv.client.CLibrary
import mihon.video.engine.mpv.client.LibMpv
import mihon.video.engine.mpv.render.MemoryFrames
import mihon.video.engine.mpv.render.RenderParams
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * mpv's OpenGL renderer in an OpenGL context of its own, off any window, with each frame read back into memory.
 *
 * Its context is EGL's, where mpv maps VA-API's decoded frames as textures without copying them. The libmpv the app
 * carries opens VA-API on a DRM render node only, never through the window system, so it gets the render node of the
 * GPU to decode on.
 */
internal class EglFrames private constructor(
    private val mpv: LibMpv,
    override val renderContext: Pointer,
    private val context: EglContext,
    private val gl: GlFunctions,
    private val renderNode: Int,
    /** What the render context was made with: mpv keeps pointing at the render node's parameters while it exists. */
    private val creation: RenderParams,
) : MemoryFrames {
    override val name = "OpenGL"

    private var framebuffer = 0
    private var renderbuffer = 0
    private var width = 0
    private var height = 0
    private var params: Memory? = null
    private var paramValues: RenderParams? = null

    override fun render(into: Pointer, stride: Int, width: Int, height: Int) {
        val params = params?.takeIf { width == this.width && height == this.height } ?: resize(width, height) ?: return
        mpv.renderContextRender(renderContext, params)
        gl.readPixels(framebuffer, width, height, into, stride)
    }

    override fun free() {
        mpv.renderContextFree(renderContext)
        deleteTarget()
        context.destroy()
        if (renderNode >= 0) CLibrary.INSTANCE.close(renderNode)
    }

    private fun resize(width: Int, height: Int): Memory? {
        deleteTarget()
        val framebuffer = gl.genFramebuffer()
        val renderbuffer = gl.genRenderbuffer()
        if (!gl.attachRenderbuffer(framebuffer, renderbuffer, width, height)) {
            gl.deleteFramebuffer(framebuffer)
            gl.deleteRenderbuffer(renderbuffer)
            logcat(LogPriority.ERROR) { "No framebuffer of ${width}x$height for mpv to draw into" }
            return null
        }
        this.framebuffer = framebuffer
        this.renderbuffer = renderbuffer
        this.width = width
        this.height = height
        val values = RenderParams()
            .ints(RenderParams.OPENGL_FBO, framebuffer, width, height, 0)
            // Unflipped, mpv draws a framebuffer's rows top first, as glReadPixels reads them
            .int(RenderParams.FLIP_Y, 0)
            .int(RenderParams.BLOCK_FOR_TARGET_TIME, 0)
        paramValues = values
        return values.build().also { params = it }
    }

    private fun deleteTarget() {
        if (framebuffer != 0) gl.deleteFramebuffer(framebuffer)
        if (renderbuffer != 0) gl.deleteRenderbuffer(renderbuffer)
        framebuffer = 0
        renderbuffer = 0
        params = null
        paramValues = null
    }

    companion object {
        /** Null where EGL offers no OpenGL context without a window, as with drivers other than Mesa's. */
        fun create(mpv: LibMpv, handle: Pointer): EglFrames? {
            val context = EglContext.create() ?: return null
            val gl = GlFunctions(Egl.INSTANCE::eglGetProcAddress)
            val renderNode = openRenderNode()
            val params = RenderParams()
                .string(RenderParams.API_TYPE, "opengl")
                .memory(RenderParams.OPENGL_INIT_PARAMS, gl.mpvInitParams())
            if (renderNode >= 0) params.memory(RenderParams.DRM_DISPLAY_V2, drmParams(renderNode))
            val result = PointerByReference()
            val status = mpv.renderContextCreate(result, handle, params.build())
            if (status < 0) {
                logcat(LogPriority.ERROR) { "mpv's OpenGL renderer didn't start: ${mpv.errorString(status)}" }
                context.destroy()
                if (renderNode >= 0) CLibrary.INSTANCE.close(renderNode)
                return null
            }
            return EglFrames(mpv, result.value, context, gl, renderNode, params)
        }

        /** The first DRM render node, or -1 when none opens. */
        private fun openRenderNode(): Int {
            val nodes = File("/dev/dri").listFiles { file -> file.name.startsWith("renderD") }.orEmpty().sorted()
            return nodes.firstNotNullOfOrNull { node ->
                CLibrary.INSTANCE.open(node.path, CLibrary.O_RDWR or CLibrary.O_CLOEXEC).takeIf { it >= 0 }
            } ?: -1
        }

        /** `mpv_opengl_drm_params_v2` naming only the render node: no display, CRTC, connector or atomic request. */
        private fun drmParams(renderNode: Int) = Memory(DRM_PARAMS_SIZE).apply {
            clear()
            setInt(0, -1)
            setInt(4, -1)
            setInt(8, -1)
            setInt(DRM_PARAMS_RENDER_FD, renderNode)
        }

        // fd, crtc_id, connector_id, then atomic_request_ptr at 16 and render_fd at 24 on x86_64
        private const val DRM_PARAMS_SIZE = 32L
        private const val DRM_PARAMS_RENDER_FD = 24L
    }
}

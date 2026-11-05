package mihon.video.engine.mpv.render

import com.sun.jna.CallbackThreadInitializer
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import logcat.LogPriority
import mihon.video.engine.mpv.client.LibMpv
import mihon.video.engine.mpv.client.RenderUpdateCallback
import mihon.video.engine.mpv.render.gl.EglFrames
import mihon.video.engine.spi.player.ComposeVideoOutput
import org.jetbrains.skia.Image
import tachiyomi.core.common.util.system.logcat
import java.awt.EventQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Renders one mpv instance's picture through mpv's render API: a render context of the instance's, on a thread of its
 * own, from [start] to [release]. mpv draws with it once its `vo` is `libmpv`, which needs the render context to exist
 * first, and has to be set back to `null` before the render context goes.
 *
 * The render context draws off any window, so it lives as long as the instance and outputs only come and go: mpv picks
 * its hardware decoding when a file opens, and only takes VA-API's frames without a copy if its output can already
 * show them then. mpv renders whole frames into memory, [FrameSlots], that the attached output's window draws from:
 * only when it has a new frame, or the output changed, and the window draws again only then. It renders with OpenGL
 * off any window ([EglFrames]), where VA-API's frames need no copy and the GPU scales, and with its software renderer
 * where that can't start. On a screen the toolkit draws at a larger scale than the screen's, frames render at the
 * screen's ([renderSize]). With no output, frames still render, into a few pixels nothing reads: mpv's output waits
 * for each frame to be rendered before showing the next.
 *
 * Compose draws a window's frame by recording it on the UI thread and replaying the recording later inside its own
 * renderer, whose OpenGL context it offers no way into, so mpv can't draw straight into the window's drawing.
 */
internal class MpvRenderer(
    private val mpv: LibMpv,
    private val handle: Pointer,
) : ComposeVideoOutput.FrameSource {
    private val lock = ReentrantLock()
    private val wake = lock.newCondition()

    // Under the lock.
    private var pending = false
    private var running = true

    private val onUpdate = RenderUpdateCallback { signal() }.also {
        Native.setCallbackThreadInitializer(it, CallbackThreadInitializer(true, false, "mpv-render-update"))
    }

    @Volatile
    private var output: ComposeVideoOutput? = null

    /** The render thread's newest slots. */
    @Volatile
    private var slots: FrameSlots? = null

    // On the UI thread.
    private var drawnSlots: FrameSlots? = null
    private var image: Image? = null
    private var imageSequence = -1L

    /** Slots the render thread replaced, which the UI closes once it no longer takes from them. */
    private val retired = mutableListOf<FrameSlots>()

    private var thread: Thread? = null

    /** Creates the render context; returns once it exists, or false if it can't be created. */
    fun start(): Boolean {
        val created = CompletableFuture<Boolean>()
        thread = Thread({ run(created) }, "${Thread.currentThread().name}-render").apply {
            isDaemon = true
            start()
        }
        return created.join()
    }

    /** Draws into [output] from now on, in place of any output before. */
    fun attach(output: ComposeVideoOutput) {
        if (output === this.output) return
        detach()
        this.output = output
        output.attach(this)
        signal()
    }

    fun detach() {
        val output = output ?: return
        this.output = null
        output.detach(this)
    }

    /** Frees the render context and stops drawing into any output; returns once it is freed. */
    fun release() {
        detach()
        lock.withLock {
            running = false
            wake.signal()
        }
        thread?.join()
        val leftovers = synchronized(retired) { retired.toList().also { retired.clear() } } + listOfNotNull(slots)
        slots = null
        EventQueue.invokeLater {
            image?.close()
            image = null
            leftovers.forEach(FrameSlots::close)
        }
    }

    override fun frame(): Image? {
        val current = slots ?: return image
        if (current !== drawnSlots) {
            drawnSlots = current
            imageSequence = -1L
        }
        // Slots replaced after [current] was read stay until the next drawing
        synchronized(retired) {
            retired.removeAll { replaced -> (replaced !== current).also { if (it) replaced.close() } }
        }
        val (bitmap, sequence) = current.take() ?: return image
        if (sequence != imageSequence) {
            image?.close()
            image = Image.makeFromBitmap(bitmap)
            imageSequence = sequence
        }
        return image
    }

    override fun onResize() = signal()

    private fun signal() {
        lock.withLock {
            pending = true
            wake.signal()
        }
    }

    private fun run(created: CompletableFuture<Boolean>) {
        val frames = EglFrames.create(mpv, handle) ?: SoftwareFrames.create(mpv, handle)
        created.complete(frames != null)
        if (frames == null) return
        logcat(LogPriority.INFO) { "mpv renders with its ${frames.name} renderer" }
        mpv.renderContextSetUpdateCallback(frames.renderContext, onUpdate, null)
        val unseen = Memory(UNSEEN_SIZE * UNSEEN_SIZE * BYTES_PER_PIXEL.toLong())
        var rendered: Pair<ComposeVideoOutput, Pair<Int, Int>>? = null
        var hasFrame = false
        while (true) {
            lock.withLock {
                while (!pending && running) wake.await()
                pending = false
            }
            if (!lock.withLock { running }) break
            val newFrame = mpv.renderContextUpdate(frames.renderContext) and RenderParams.UPDATE_FRAME != 0L
            hasFrame = hasFrame || newFrame
            val output = output
            if (output == null) {
                if (newFrame) {
                    frames.render(unseen, UNSEEN_SIZE * BYTES_PER_PIXEL, UNSEEN_SIZE, UNSEEN_SIZE)
                    mpv.renderContextReportSwap(frames.renderContext)
                }
                continue
            }
            val size = targetSize(output)
            if (size.first <= 0 || size.second <= 0 || !hasFrame || (!newFrame && rendered == output to size)) continue
            val current = slots?.takeIf { it.width == size.first && it.height == size.second }
                ?: FrameSlots(size.first, size.second).also { replacement ->
                    slots?.let { synchronized(retired) { retired += it } }
                    slots = replacement
                }
            val slot = current.acquire()
            val pixels = current.bitmap(slot).peekPixels() ?: continue
            frames.render(Pointer(pixels.addr), pixels.rowBytes, size.first, size.second)
            mpv.renderContextReportSwap(frames.renderContext)
            current.publish(slot)
            rendered = output to size
            output.frameReady()
        }
        mpv.renderContextSetUpdateCallback(frames.renderContext, null, null)
        frames.free()
    }

    private fun targetSize(output: ComposeVideoOutput): Pair<Int, Int> {
        val configuration = output.window.graphicsConfiguration ?: return output.width to output.height
        return renderSize(output.width, output.height, configuration.effectiveScale, configuration.screenScale)
    }

    private companion object {
        /** The side of the frames rendered with no output, which only have to be rendered, not seen. */
        const val UNSEEN_SIZE = 16
        const val BYTES_PER_PIXEL = 4
    }
}

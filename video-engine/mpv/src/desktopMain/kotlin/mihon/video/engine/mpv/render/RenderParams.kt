package mihon.video.engine.mpv.render

import com.sun.jna.Memory
import com.sun.jna.Pointer

/**
 * A NULL-terminated `mpv_render_param` array: each entry a type and a pointer to its value. The values are native
 * memory of their own, kept here for as long as mpv may read them.
 */
internal class RenderParams {
    private val entries = mutableListOf<Pair<Int, Pointer?>>()
    private val values = mutableListOf<Memory>()

    fun string(type: Int, value: String): RenderParams = add(type, nativeString(value))

    fun int(type: Int, value: Int): RenderParams = add(type, Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, value) })

    fun ints(type: Int, vararg value: Int): RenderParams = add(
        type,
        Memory(Int.SIZE_BYTES.toLong() * value.size).apply {
            value.forEachIndexed { index, item -> setInt(Int.SIZE_BYTES.toLong() * index, item) }
        },
    )

    fun long(type: Int, value: Long): RenderParams =
        add(type, Memory(Long.SIZE_BYTES.toLong()).apply { setLong(0, value) })

    fun memory(type: Int, value: Memory): RenderParams = add(type, value)

    /** A value that isn't the array's own, such as the frame's pixels. */
    fun pointer(type: Int, value: Pointer): RenderParams {
        entries += type to value
        return this
    }

    /** The array, valid while this object is reachable. */
    fun build(): Memory = Memory(ENTRY_SIZE * (entries.size + 1)).apply {
        clear()
        entries.forEachIndexed { index, (type, value) ->
            setInt(ENTRY_SIZE * index, type)
            setPointer(ENTRY_SIZE * index + Long.SIZE_BYTES, value)
        }
        values += this
    }

    private fun add(type: Int, value: Memory): RenderParams {
        values += value
        entries += type to value
        return this
    }

    companion object {
        // mpv_render_param: an int type, padded, then a pointer
        private const val ENTRY_SIZE = 16L

        // mpv_render_param_type
        const val API_TYPE = 1
        const val OPENGL_INIT_PARAMS = 2
        const val OPENGL_FBO = 3
        const val FLIP_Y = 4
        const val BLOCK_FOR_TARGET_TIME = 12
        const val DRM_DISPLAY_V2 = 16
        const val SW_SIZE = 17
        const val SW_FORMAT = 18
        const val SW_STRIDE = 19
        const val SW_POINTER = 20

        /** `mpv_render_context_update`'s bit for a new frame to render. */
        const val UPDATE_FRAME = 1L

        fun nativeString(value: String): Memory {
            val bytes = value.encodeToByteArray()
            return Memory(bytes.size + 1L).apply {
                write(0, bytes, 0, bytes.size)
                setByte(bytes.size.toLong(), 0)
            }
        }
    }
}

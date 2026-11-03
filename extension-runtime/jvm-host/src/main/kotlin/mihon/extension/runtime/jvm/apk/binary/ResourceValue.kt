package mihon.extension.runtime.jvm.apk.binary

/**
 * A typed value of a compiled resource file (`Res_value`): an XML attribute's or a resource table entry's. [data] is a
 * string pool index for a [STRING], a resource ID for a [REFERENCE], and the value itself for the other types.
 */
internal data class ResourceValue(val type: Int, val data: Int) {

    val isInt: Boolean
        get() = type in FIRST_INT..LAST_INT

    companion object {
        const val NULL = 0x00
        const val REFERENCE = 0x01
        const val STRING = 0x03
        const val FLOAT = 0x04

        /** Decimal, hexadecimal, boolean and colour values, all of which Android reads as ints. */
        const val FIRST_INT = 0x10
        const val INT_BOOLEAN = 0x12
        const val LAST_INT = 0x1f

        /** The value at [offset] in [chunk], laid out as `size`, `res0`, `dataType`, `data`. */
        fun at(chunk: ResourceChunk, offset: Int): ResourceValue = ResourceValue(
            chunk.u8(offset + 3),
            chunk.s32(offset + 4),
        )
    }
}

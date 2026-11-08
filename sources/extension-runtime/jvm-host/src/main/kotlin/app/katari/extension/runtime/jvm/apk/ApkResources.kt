package app.katari.extension.runtime.jvm.apk

import app.katari.extension.runtime.jvm.apk.binary.ResourceChunk
import app.katari.extension.runtime.jvm.apk.binary.ResourceStringPool
import app.katari.extension.runtime.jvm.apk.binary.ResourceValue

/**
 * The few resources a host reads from an apk's `resources.arsc`: a string, such as the application label, and the
 * file of a bitmap, such as the launcher icon, in its sharpest density. Values are read from the configuration with no
 * qualifiers when there is one, as a device whose locale no translation matches would read them.
 */
internal class ApkResources(table: ByteArray) {

    private class TypeChunk(val chunk: ResourceChunk, val density: Int, val hasQualifiers: Boolean)

    private val strings: ResourceStringPool?
    private val packageId: Int
    private val typeChunks: Map<Int, List<TypeChunk>>

    init {
        val root = ResourceChunk.of(table)
        require(root.type == ResourceChunk.TABLE) { "Not a resource table" }
        var strings: ResourceStringPool? = null
        var packageChunk: ResourceChunk? = null
        for (chunk in root.children()) {
            when (chunk.type) {
                ResourceChunk.STRING_POOL -> strings = ResourceStringPool(chunk)
                // An extension has one package, its own; shared libraries' would come later
                ResourceChunk.TABLE_PACKAGE -> if (packageChunk == null) packageChunk = chunk
            }
        }
        this.strings = strings
        packageId = packageChunk?.s32(8) ?: 0
        typeChunks = packageChunk?.children()
            .orEmpty()
            .filter { it.type == ResourceChunk.TABLE_TYPE }
            .map { chunk ->
                val config = 20
                val configSize = chunk.s32(config)
                // Everything after the size field is a qualifier; a configuration with none of them set is the default
                val hasQualifiers = (4 until configSize).any { chunk.u8(config + it) != 0 }
                chunk.u8(8) to TypeChunk(chunk, density = chunk.u16(config + 14), hasQualifiers = hasQualifiers)
            }
            .groupBy({ it.first }, { it.second })
    }

    /** The text of the string resource [resourceId], following references to other resources. */
    fun string(resourceId: Int): String? {
        val value = resolve(resourceId) ?: return null
        return if (value.type == ResourceValue.STRING) strings?.get(value.data.toLong() and 0xffffffffL) else null
    }

    /** The value of [resourceId], following references to other resources, `null` when it has no plain value. */
    fun resolve(resourceId: Int): ResourceValue? {
        var id = resourceId
        repeat(MAX_REFERENCE_DEPTH) {
            val candidates = values(id)
            val value = candidates.firstOrNull { !it.first.hasQualifiers }?.second ?: candidates.firstOrNull()?.second
            if (value?.type != ResourceValue.REFERENCE) return value
            id = value.data
        }
        return null
    }

    /** The path in the apk of [resourceId]'s bitmap of the highest density, `null` when it has none, such as an XML one. */
    fun bitmapFile(resourceId: Int): String? {
        return values(resourceId)
            .mapNotNull { (type, value) ->
                val path = value.takeIf { it.type == ResourceValue.STRING }
                    ?.let { strings?.get(it.data.toLong() and 0xffffffffL) }
                    ?.takeIf { path -> BITMAP_SUFFIXES.any { path.endsWith(it, ignoreCase = true) } }
                    ?: return@mapNotNull null
                path to type.density.let { if (it == 0) DEFAULT_DENSITY else it }
            }
            .filter { (_, density) -> density < DENSITY_ANY }
            .maxByOrNull { (_, density) -> density }
            ?.first
    }

    private fun values(resourceId: Int): List<Pair<TypeChunk, ResourceValue>> {
        if (resourceId ushr 24 != packageId) return emptyList()
        val types = typeChunks[(resourceId shr 16) and 0xff].orEmpty()
        return types.mapNotNull { type -> entry(type.chunk, resourceId and 0xffff)?.let { type to it } }
    }

    private fun entry(chunk: ResourceChunk, index: Int): ResourceValue? {
        val flags = chunk.u8(9)
        val entryCount = chunk.s32(12)
        val entriesStart = chunk.s32(16)
        val offsets = chunk.headerSize
        val offset = when {
            flags and FLAG_SPARSE != 0 -> (0 until entryCount)
                .firstOrNull { chunk.u16(offsets + it * 4) == index }
                ?.let { chunk.u16(offsets + it * 4 + 2) * 4 }
            index >= entryCount -> null
            flags and FLAG_OFFSET16 != 0 -> chunk.u16(offsets + index * 2).takeIf { it != 0xffff }?.let { it * 4 }
            else -> chunk.s32(offsets + index * 4).takeIf { it != -1 }
        } ?: return null
        val at = entriesStart + offset
        val entryFlags = chunk.u16(at + 2)
        return when {
            // Android 14's compact entries keep the value's type in the flags' high byte and its data after them
            entryFlags and ENTRY_COMPACT != 0 -> ResourceValue(entryFlags ushr 8, chunk.s32(at + 4))
            // A bag, such as a style, has no single value
            entryFlags and ENTRY_COMPLEX != 0 -> null
            else -> ResourceValue.at(chunk, at + chunk.u16(at))
        }
    }

    private companion object {
        const val FLAG_SPARSE = 0x01
        const val FLAG_OFFSET16 = 0x02
        const val ENTRY_COMPLEX = 0x0001
        const val ENTRY_COMPACT = 0x0008

        /** What Android takes a resource without a density qualifier to be drawn at. */
        const val DEFAULT_DENSITY = 160

        /** `anydpi` and `nodpi`, which hold drawables of no density. */
        const val DENSITY_ANY = 0xfffe

        const val MAX_REFERENCE_DEPTH = 8
        val BITMAP_SUFFIXES = listOf(".png", ".webp", ".jpg", ".jpeg")
    }
}

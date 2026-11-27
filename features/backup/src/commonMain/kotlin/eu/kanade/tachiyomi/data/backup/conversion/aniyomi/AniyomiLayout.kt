package eu.kanade.tachiyomi.data.backup.conversion.aniyomi

import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_VARINT
import eu.kanade.tachiyomi.data.backup.stream.decodeBackup
import eu.kanade.tachiyomi.data.backup.stream.lengthDelimitedTag
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource

/**
 * Where an Aniyomi backup keeps its anime and its extension APKs. Aniyomi 0.18.1 moved the anime part from 3, 4, 103 and
 * 106-109 to 501-506 and marked the new layout with `isLegacy` at 500, which it writes as false; an older backup has no
 * field 500 (research/02).
 */
internal enum class AniyomiLayout(val animeTag: Long, val extensionApkTag: Long) {
    CURRENT(lengthDelimitedTag(501), lengthDelimitedTag(504)) {
        override fun decode(parser: ProtoBuf, bytes: ByteArray) =
            parser.decodeBackup(AniyomiCurrentBackup.serializer(), bytes).toAniyomiBackup()
    },
    LEGACY(lengthDelimitedTag(3), lengthDelimitedTag(106)) {
        override fun decode(parser: ProtoBuf, bytes: ByteArray) =
            parser.decodeBackup(AniyomiLegacyBackup.serializer(), bytes).toAniyomiBackup()
    },
    ;

    /** The backup without its entries and extension APKs, as [bytes] hold it. */
    abstract fun decode(parser: ProtoBuf, bytes: ByteArray): AniyomiBackup

    companion object {
        private const val IS_LEGACY_TAG = (500L shl 3) or WIRE_TYPE_VARINT.toLong()

        fun of(content: BufferedSource): AniyomiLayout {
            val input = ProtoInput(content)
            while (!input.exhausted()) {
                val tag = input.readTag()
                if (tag == IS_LEGACY_TAG) return CURRENT
                input.skipField(tag)
            }
            return LEGACY
        }
    }
}

package eu.kanade.tachiyomi.data.backup.conversion.newer

import eu.kanade.tachiyomi.data.backup.stream.MalformedBackupException
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_FIXED32
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_FIXED64
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_LENGTH_DELIMITED
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_VARINT
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.protobuf.ProtoNumber
import okio.Buffer

/**
 * What a protobuf message of another app's backup holds, as far as Katari knows it: the fields of the models it is read
 * as ([readAs], such as an entry's full model and the part read before converting it), nested messages included; the
 * fields of each message Katari knows and doesn't read ([ignored], by the message's model); and the messages it doesn't
 * look into ([notRead]), such as trackers, which are left behind whole. [check] walks a message's bytes against it, so
 * a newer app's additions are found rather than skipped unseen (D123), and a field that changed type is found before
 * decoding it fails (D124).
 */
internal class MessageShape(
    private val readAs: List<SerialDescriptor>,
    ignored: Map<SerialDescriptor, Set<Int>> = emptyMap(),
    notRead: Set<SerialDescriptor> = emptySet(),
) {
    // By name, as a nullable field's descriptor is a different object from its class's
    private val ignored = ignored.mapKeys { it.key.serialName }
    private val notRead = notRead.mapTo(hashSetOf()) { it.serialName.removeSuffix("?") }

    /**
     * Records in [reading] what of [bytes], a message at [where], Katari doesn't know. Throws
     * [MalformedBackupException] when the bytes don't form a message at all, as a backup cut short or corrupt doesn't.
     */
    fun check(bytes: ByteArray, where: List<String>, reading: NewerAppReading) {
        check(readAs.first(), topFields, bytes, where, reading)
    }

    // The first model's own field wins where two declare a number
    private val topFields by lazy {
        readAs.reversed().fold(emptyMap<Int, Field>()) { all, model -> all + fieldsOf(model) }
    }

    private fun check(
        descriptor: SerialDescriptor,
        fields: Map<Int, Field>,
        bytes: ByteArray,
        where: List<String>,
        parts: NewerAppReading,
    ) {
        val ignoredHere = ignored[descriptor.name].orEmpty()
        val input = ProtoInput(Buffer().write(bytes))
        val unknown = mutableMapOf<Int, Int>()
        val changed = mutableMapOf<Int, Pair<Set<Int>, Int>>()
        while (!input.exhausted()) {
            val tag = input.readTag()
            val number = (tag ushr 3).toInt()
            val wireType = (tag and 0x7).toInt()
            val field = fields[number]
            when {
                field == null -> {
                    if (number !in ignoredHere) unknown[number] = wireType
                    input.skipField(tag)
                }
                wireType !in field.wireTypes -> {
                    changed[number] = field.wireTypes to wireType
                    input.skipField(tag)
                }
                field.message != null && wireType == WIRE_TYPE_LENGTH_DELIMITED && field.message.name !in notRead -> {
                    val inner = input.readByteArray(input.readLength())
                    check(field.message, fieldsOf(field.message), inner, where + field.name, parts)
                }
                else -> input.skipField(tag)
            }
        }
        // Counted once per message, so a part shows how many entries or chapters hold it
        unknown.forEach { (number, wireType) -> parts.unknown(where, number.toString(), wireTypeName(wireType)) }
        changed.forEach { (number, types) ->
            val (expected, found) = types
            val expectedNames = expected.joinToString(" or ", transform = ::wireTypeName)
            parts.changed(where, number.toString(), expectedNames, wireTypeName(found))
        }
    }

    private class Field(val name: String, val wireTypes: Set<Int>, val message: SerialDescriptor?)

    private val fieldCache = mutableMapOf<SerialDescriptor, Map<Int, Field>>()

    private fun fieldsOf(descriptor: SerialDescriptor): Map<Int, Field> = fieldCache.getOrPut(descriptor) {
        (0 until descriptor.elementsCount).associate { index ->
            val number = descriptor.getElementAnnotations(index).filterIsInstance<ProtoNumber>().firstOrNull()?.number
                ?: (index + 1)
            val element = descriptor.getElementDescriptor(index)
            number to Field(descriptor.getElementName(index), wireTypesOf(element), messageOf(element))
        }
    }

    // A message, or each message of a list, that the walk goes into
    private val SerialDescriptor.name: String
        get() = serialName.removeSuffix("?")

    private fun messageOf(element: SerialDescriptor): SerialDescriptor? = when (element.kind) {
        StructureKind.CLASS, StructureKind.OBJECT -> element
        StructureKind.LIST -> element.getElementDescriptor(0).takeIf { it.kind == StructureKind.CLASS }
        else -> null
    }

    private fun wireTypesOf(element: SerialDescriptor): Set<Int> = when (val kind = element.kind) {
        // A list of numbers may be packed into one length-delimited field or written one number per field
        StructureKind.LIST -> setOf(WIRE_TYPE_LENGTH_DELIMITED) + element.getElementDescriptor(0).let { item ->
            if (item.kind == StructureKind.CLASS) emptySet() else scalarWireTypes(item.kind)
        }
        else -> scalarWireTypes(kind)
    }

    private fun scalarWireTypes(kind: SerialKind): Set<Int> = when (kind) {
        PrimitiveKind.BOOLEAN, PrimitiveKind.BYTE, PrimitiveKind.CHAR, PrimitiveKind.SHORT, PrimitiveKind.INT,
        PrimitiveKind.LONG, SerialKind.ENUM,
        -> setOf(WIRE_TYPE_VARINT)
        PrimitiveKind.FLOAT -> setOf(WIRE_TYPE_FIXED32)
        PrimitiveKind.DOUBLE -> setOf(WIRE_TYPE_FIXED64)
        PrimitiveKind.STRING, StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP, StructureKind.LIST,
        is PolymorphicKind, SerialKind.CONTEXTUAL,
        -> setOf(WIRE_TYPE_LENGTH_DELIMITED)
    }
}

/** How the issue names a wire type, as protobuf's own documentation does. */
private fun wireTypeName(wireType: Int): String = when (wireType) {
    WIRE_TYPE_VARINT -> "varint"
    WIRE_TYPE_FIXED64 -> "fixed64"
    WIRE_TYPE_LENGTH_DELIMITED -> "length-delimited"
    WIRE_TYPE_FIXED32 -> "fixed32"
    else -> "wire type $wireType"
}

package eu.kanade.tachiyomi.data.backup.conversion.newer

import eu.kanade.tachiyomi.data.backup.conversion.mihon.MihonManga
import eu.kanade.tachiyomi.data.backup.conversion.mihon.mihonEntryShape
import eu.kanade.tachiyomi.data.backup.stream.MalformedBackupException
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_LENGTH_DELIMITED
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_VARINT
import eu.kanade.tachiyomi.data.backup.stream.writeVarint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer
import org.junit.jupiter.api.Test

class NewerAppReadingTest {

    @Test
    fun `an entry a newer app changed is left behind and listed, while its unknown parts are counted`() {
        val reading = NewerAppReading("Mihon")
        val shape = mihonEntryShape()
        val readable = entry {
            text(2, "/title/1")
            text(3, "Frieren")
            number(42, 1)
        }
        // Genre at 7 is text; a newer app writing a number there makes the entry undecodable
        val changed = entry {
            text(2, "/title/2")
            text(3, "Dandadan")
            number(7, 5)
            number(42, 1)
        }

        val first = reading.protobufEntry(ProtoBuf, shape, MihonManga.serializer(), readable)
        val second = reading.protobufEntry(ProtoBuf, shape, MihonManga.serializer(), changed)

        first?.title shouldBe "Frieren"
        second shouldBe null
        val findings = reading.findings()
        findings.unknownParts shouldContainExactly listOf(UnknownPart(IN_ENTRY, "42", "varint", times = 2))
        findings.changedParts.map { it.name to it.times } shouldContainExactly listOf("7" to 1)
        findings.unreadableEntries.map { it.position to it.title } shouldContainExactly listOf(2 to "Dandadan")
    }

    @Test
    fun `an entry cut short fails as damaged rather than being left behind as a newer app's`() {
        val reading = NewerAppReading("Mihon")
        val whole = entry {
            text(2, "/title/1")
            text(3, "Frieren")
        }

        shouldThrow<MalformedBackupException> {
            reading.protobufEntry(ProtoBuf, mihonEntryShape(), MihonManga.serializer(), whole.copyOf(whole.size - 3))
        }
    }

    private class EntryBytes(val buffer: Buffer) {
        fun text(number: Int, value: String) {
            buffer.writeVarint(number.toLong() shl 3 or WIRE_TYPE_LENGTH_DELIMITED.toLong())
            val bytes = value.encodeToByteArray()
            buffer.writeVarint(bytes.size.toLong())
            buffer.write(bytes)
        }

        fun number(number: Int, value: Long) {
            buffer.writeVarint(number.toLong() shl 3 or WIRE_TYPE_VARINT.toLong())
            buffer.writeVarint(value)
        }
    }

    // Source 1 at field 1, then the fields the test writes
    private fun entry(fields: EntryBytes.() -> Unit): ByteArray {
        val bytes = EntryBytes(Buffer())
        bytes.number(1, 1)
        bytes.fields()
        return bytes.buffer.readByteArray()
    }
}

package mihon.script.runtime.web

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Decoding for `TextDecoder`: Encoding Standard labels, decoded with Java's charsets. Labels Java doesn't know throw a
 * `RangeError` in JavaScript, as the standard asks for an unknown label.
 */
internal object TextEncodings {

    /** The Encoding Standard's name for [label] and the charset that decodes it, or null when Java has none. */
    fun forLabel(label: String): Pair<String, Charset>? {
        val normalized = label.trim().lowercase()
        val name = ALIASES[normalized] ?: normalized
        // Android's charsets differ from the desktop JDK's, so the standard's own name is tried too
        val charset = listOfNotNull(JAVA_NAMES[name], name)
            .firstNotNullOfOrNull { runCatching { Charset.forName(it) }.getOrNull() }
            ?: return null
        return name to charset
    }

    fun decode(bytes: ByteArray, charset: Charset, fatal: Boolean, ignoreBom: Boolean): String {
        val bomLength = if (ignoreBom) 0 else bomLength(bytes, charset)
        val action = if (fatal) CodingErrorAction.REPORT else CodingErrorAction.REPLACE
        val decoder = charset.newDecoder().onMalformedInput(action).onUnmappableCharacter(action)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes, bomLength, bytes.size - bomLength)).toString()
        } catch (e: CharacterCodingException) {
            throw IllegalArgumentException("The encoded data was not valid for encoding ${charset.name()}", e)
        }
    }

    private fun bomLength(bytes: ByteArray, charset: Charset): Int {
        fun startsWith(vararg bom: Int) = bytes.size >= bom.size && bom.indices.all { bytes[it] == bom[it].toByte() }
        return when (charset.name()) {
            "UTF-8" -> if (startsWith(0xEF, 0xBB, 0xBF)) 3 else 0
            "UTF-16LE" -> if (startsWith(0xFF, 0xFE)) 2 else 0
            "UTF-16BE" -> if (startsWith(0xFE, 0xFF)) 2 else 0
            else -> 0
        }
    }

    // Where the Encoding Standard's name isn't one Java accepts
    private val JAVA_NAMES = mapOf(
        "big5" to "Big5-HKSCS",
        "shift_jis" to "windows-31j",
        "euc-kr" to "x-windows-949",
    )

    // Labels whose Encoding Standard name differs from the label (https://encoding.spec.whatwg.org/#names-and-labels)
    private val ALIASES = buildMap {
        listOf("unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8", "utf8", "x-unicode20utf8")
            .forEach { put(it, "utf-8") }
        listOf(
            "ansi_x3.4-1968", "ascii", "cp1252", "cp819", "csisolatin1", "ibm819", "iso-8859-1", "iso-ir-100",
            "iso8859-1", "iso88591", "iso_8859-1", "iso_8859-1:1987", "l1", "latin1", "us-ascii", "x-cp1252",
        ).forEach { put(it, "windows-1252") }
        listOf("chinese", "csgb2312", "csiso58gb231280", "gb2312", "gb_2312", "gb_2312-80", "iso-ir-58", "x-gbk")
            .forEach { put(it, "gbk") }
        listOf("big5-hkscs", "cn-big5", "csbig5", "x-x-big5").forEach { put(it, "big5") }
        listOf("csshiftjis", "ms932", "ms_kanji", "shift-jis", "sjis", "windows-31j", "x-sjis")
            .forEach { put(it, "shift_jis") }
        listOf(
            "cseuckr", "csksc56011987", "iso-ir-149", "korean", "ks_c_5601-1987", "ks_c_5601-1989", "ksc5601",
            "ksc_5601", "windows-949",
        ).forEach { put(it, "euc-kr") }
        listOf("csunicode", "iso-10646-ucs-2", "ucs-2", "unicode", "unicodefeff", "utf-16")
            .forEach { put(it, "utf-16le") }
        put("unicodefffe", "utf-16be")
        listOf("cskoi8r", "koi", "koi8", "koi8_r").forEach { put(it, "koi8-r") }
        listOf("cp1251", "x-cp1251").forEach { put(it, "windows-1251") }
    }
}

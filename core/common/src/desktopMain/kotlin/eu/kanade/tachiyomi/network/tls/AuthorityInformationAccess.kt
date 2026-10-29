package eu.kanade.tachiyomi.network.tls

import java.security.cert.X509Certificate

/**
 * The `http` addresses of the certificate's issuer from its Authority Information Access extension (RFC 5280
 * §4.2.2.1), where a certificate authority publishes the certificate that signed this one.
 */
internal fun X509Certificate.caIssuerUrls(): List<String> {
    val extension = getExtensionValue(AUTHORITY_INFO_ACCESS_OID) ?: return emptyList()
    return try {
        // The extension value is the DER of an OCTET STRING wrapping the SEQUENCE OF AccessDescription
        val accessDescriptions = DerReader(DerReader(extension).read(OCTET_STRING)).read(SEQUENCE)
        val descriptions = DerReader(accessDescriptions)
        buildList {
            while (descriptions.hasMore()) {
                val description = DerReader(descriptions.read(SEQUENCE))
                val method = description.read(OBJECT_IDENTIFIER)
                val location = description.readAny()
                if (method.contentEquals(CA_ISSUERS_OID) && location.tag == URI_GENERAL_NAME) {
                    val url = location.content.toString(Charsets.US_ASCII)
                    if (url.startsWith("http://", ignoreCase = true)) add(url)
                }
            }
        }
    } catch (_: IllegalArgumentException) {
        emptyList()
    }
}

/** Definite-length DER, which is all X.509 uses; throws [IllegalArgumentException] on anything else. */
private class DerReader(private val bytes: ByteArray) {
    private var position = 0

    class Element(val tag: Int, val content: ByteArray)

    fun hasMore(): Boolean = position < bytes.size

    fun read(tag: Int): ByteArray {
        val element = readAny()
        require(element.tag == tag) { "Expected tag $tag, found ${element.tag}" }
        return element.content
    }

    fun readAny(): Element {
        val tag = next()
        var length = next()
        if (length and 0x80 != 0) {
            val octets = length and 0x7f
            require(octets in 1..4) { "Unsupported length" }
            length = 0
            repeat(octets) { length = (length shl 8) or next() }
        }
        require(length >= 0 && position + length <= bytes.size) { "Truncated element" }
        return Element(tag, bytes.copyOfRange(position, position + length)).also { position += length }
    }

    private fun next(): Int {
        require(position < bytes.size) { "Truncated element" }
        return bytes[position++].toInt() and 0xff
    }
}

private const val AUTHORITY_INFO_ACCESS_OID = "1.3.6.1.5.5.7.1.1"
private const val SEQUENCE = 0x30
private const val OCTET_STRING = 0x04
private const val OBJECT_IDENTIFIER = 0x06

// [6] IA5String: GeneralName's uniformResourceIdentifier
private const val URI_GENERAL_NAME = 0x86

// 1.3.6.1.5.5.7.48.2, id-ad-caIssuers, as DER content
private val CA_ISSUERS_OID = byteArrayOf(0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x02)

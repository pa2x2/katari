package tachiyomi.source.local.metadata

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newGenericReader
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.InputStream
import java.nio.charset.StandardCharsets

// xmlutil's own parser rather than the one its service loader picks: the desktop app also carries xmlutil's Android
// factory, whose parser is Android's.
internal actual fun comicInfoXmlReader(stream: InputStream): XmlReader =
    xmlStreaming.newGenericReader(stream, StandardCharsets.UTF_8.name())

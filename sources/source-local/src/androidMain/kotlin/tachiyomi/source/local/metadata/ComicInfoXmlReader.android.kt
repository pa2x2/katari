package tachiyomi.source.local.metadata

import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.core.AndroidXmlReader
import java.io.InputStream
import java.nio.charset.StandardCharsets

internal actual fun comicInfoXmlReader(stream: InputStream): XmlReader =
    AndroidXmlReader(stream, StandardCharsets.UTF_8.name())

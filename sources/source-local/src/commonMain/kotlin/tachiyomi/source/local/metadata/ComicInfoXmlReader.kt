package tachiyomi.source.local.metadata

import nl.adaptivity.xmlutil.XmlReader
import java.io.InputStream

/** Reads a `ComicInfo.xml` in UTF-8 with the platform's XML parser. */
internal expect fun comicInfoXmlReader(stream: InputStream): XmlReader

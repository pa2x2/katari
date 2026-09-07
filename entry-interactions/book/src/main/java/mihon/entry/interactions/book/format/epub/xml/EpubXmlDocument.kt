package mihon.entry.interactions.book.format.epub.xml

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

/** Lets the XML parser detect the byte order mark and authored encoding before decoding text. */
internal fun ByteArray.parseEpubXml(): Document = inputStream().use { input ->
    Jsoup.parse(input, null, "", Parser.xmlParser())
}

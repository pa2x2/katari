package mihon.entry.interactions.book.format.html.prosechapter.parsing

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Many web novel sites separate paragraphs with blank lines instead of `<p>`: two `<br>` in a row, often padded with
 * more breaks or `&nbsp;` lines. Such a run among sibling nodes ends a paragraph, however long it is; a single `<br>`
 * stays a line break inside the paragraph.
 */
internal class HtmlProseParagraphBreaks private constructor(
    private val breakStarts: Set<Node>,
    private val breakNodes: Set<Node>,
) {
    val isEmpty: Boolean
        get() = breakStarts.isEmpty()

    /** Whether [node] is the first node of a paragraph break. */
    fun startsBreak(node: Node): Boolean = node in breakStarts

    /** Whether [node] belongs to a paragraph break, so it is not part of either paragraph's text. */
    fun inBreak(node: Node): Boolean = node in breakNodes

    companion object {
        val None = HtmlProseParagraphBreaks(emptySet(), emptySet())

        fun of(siblings: List<Node>): HtmlProseParagraphBreaks {
            val starts = identitySet()
            val members = identitySet()
            var index = 0
            while (index < siblings.size) {
                if (!siblings[index].isLineBreak()) {
                    index++
                    continue
                }
                var end = index
                var lineBreaks = 0
                var lastLineBreak = index
                while (end < siblings.size && (siblings[end].isLineBreak() || siblings[end].isBlankFiller())) {
                    if (siblings[end].isLineBreak()) {
                        lineBreaks++
                        lastLineBreak = end
                    }
                    end++
                }
                if (lineBreaks >= 2) {
                    starts += siblings[index]
                    members += siblings.subList(index, lastLineBreak + 1)
                }
                index = lastLineBreak + 1
            }
            return HtmlProseParagraphBreaks(starts, members)
        }

        private fun identitySet(): MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
    }
}

private fun Node.isLineBreak(): Boolean = this is Element && normalName() == "br"

// Sites pad blank lines with non-breaking and zero-width spaces, which jsoup doesn't count as whitespace.
private fun Node.isBlankFiller(): Boolean = when (this) {
    is TextNode -> wholeText.isBlankText()
    is Element -> normalName() != "img" && select("img, br").isEmpty() && text().isBlankText()
    else -> true
}

private fun String.isBlankText(): Boolean = all { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }

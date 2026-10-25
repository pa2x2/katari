package mihon.entry.interactions.book.format.html.prosechapter.css

import org.jsoup.nodes.Element

/**
 * A complex selector: compound selectors joined by descendant (` `), child (`>`), next-sibling (`+`) and
 * subsequent-sibling (`~`) combinators, matched right to left as browsers do.
 *
 * Pseudo-elements and dynamic pseudo-classes (`:hover`, `::before`, …) can never apply to a static page, so a
 * selector containing one is rejected when parsed rather than matched.
 */
internal class CssSelector private constructor(
    private val compounds: List<CssCompoundSelector>,
    private val combinators: List<Char>,
) {
    val specificity: CssSpecificity = compounds.fold(CssSpecificity.ZERO) { sum, compound ->
        sum + compound.specificity
    }

    /** The rightmost compound selector, which every match must satisfy on the element itself. */
    val subject: CssCompoundSelector get() = compounds.last()

    fun matches(element: Element): Boolean = matchesFrom(compounds.lastIndex, element)

    private fun matchesFrom(index: Int, element: Element): Boolean {
        if (!compounds[index].matches(element)) return false
        if (index == 0) return true
        return when (combinators[index - 1]) {
            ' ' -> generateSequence(element.parent()) { it.parent() }.any { matchesFrom(index - 1, it) }
            '>' -> element.parent()?.let { matchesFrom(index - 1, it) } == true
            '+' -> element.previousElementSibling()?.let { matchesFrom(index - 1, it) } == true
            '~' -> generateSequence(element.previousElementSibling()) { it.previousElementSibling() }
                .any { matchesFrom(index - 1, it) }
            else -> false
        }
    }

    companion object {
        /** Parses one selector of a selector list, or returns null when it can't apply to a static document. */
        fun parse(raw: String): CssSelector? {
            val reader = SelectorReader(raw.trim())
            val compounds = mutableListOf<CssCompoundSelector>()
            val combinators = mutableListOf<Char>()
            while (true) {
                compounds += reader.compound() ?: return null
                val hadSpace = reader.skipSpace()
                if (reader.atEnd) break
                val combinator = reader.peek().takeIf { it in ">+~" }?.also {
                    reader.next()
                    reader.skipSpace()
                } ?: if (hadSpace) ' ' else return null
                combinators += combinator
            }
            if (compounds.size > MAX_COMPOUNDS) return null
            return CssSelector(compounds, combinators)
        }

        private const val MAX_COMPOUNDS = 16
    }
}

/** Specificity as (id, class/attribute/pseudo-class, type) counts, compared in that order. */
internal data class CssSpecificity(val ids: Int, val classes: Int, val types: Int) : Comparable<CssSpecificity> {
    operator fun plus(other: CssSpecificity) =
        CssSpecificity(ids + other.ids, classes + other.classes, types + other.types)

    override fun compareTo(other: CssSpecificity): Int =
        compareValuesBy(this, other, CssSpecificity::ids, CssSpecificity::classes, CssSpecificity::types)

    companion object {
        val ZERO = CssSpecificity(0, 0, 0)
    }
}

/** A sequence of simple selectors that all apply to one element, such as `p.note[lang]:first-child`. */
internal class CssCompoundSelector(
    /** Lowercase element name, or null for `*` or none. */
    val tag: String?,
    val id: String?,
    val classes: List<String>,
    private val conditions: List<CssCondition>,
) {
    val specificity = conditions.fold(
        CssSpecificity(ids = if (id != null) 1 else 0, classes = classes.size, types = if (tag != null) 1 else 0),
    ) { sum, condition -> sum + condition.specificity }

    fun matches(element: Element): Boolean =
        (tag == null || element.normalName() == tag) &&
            (id == null || element.id() == id) &&
            classes.all(element::hasClass) &&
            conditions.all { it.matches(element) }
}

/** An attribute selector or a structural pseudo-class. */
internal sealed interface CssCondition {
    val specificity: CssSpecificity get() = CssSpecificity(0, 1, 0)

    fun matches(element: Element): Boolean

    class Attribute(
        private val name: String,
        private val operator: String?,
        private val value: String,
        private val ignoreCase: Boolean,
    ) : CssCondition {
        override fun matches(element: Element): Boolean {
            if (!element.hasAttr(name)) return false
            val actual = element.attr(name)
            val expected = value
            fun String.normal() = if (ignoreCase) lowercase() else this
            val a = actual.normal()
            val e = expected.normal()
            return when (operator) {
                null -> true
                "=" -> a == e
                "~=" -> e.isNotEmpty() && a.split(' ', '\t', '\n').contains(e)
                "|=" -> a == e || a.startsWith("$e-")
                "^=" -> e.isNotEmpty() && a.startsWith(e)
                "$=" -> e.isNotEmpty() && a.endsWith(e)
                "*=" -> e.isNotEmpty() && a.contains(e)
                else -> false
            }
        }
    }

    /** `:nth-child(an+b)` and friends; [fromEnd] counts from the last sibling, [ofType] only same-name siblings. */
    class Nth(private val a: Int, private val b: Int, private val fromEnd: Boolean, private val ofType: Boolean) :
        CssCondition {
        override fun matches(element: Element): Boolean {
            val siblings = element.parent()?.children()?.filter { !ofType || it.normalName() == element.normalName() }
                ?: listOf(element)
            val index = siblings.indexOf(element).takeIf { it >= 0 } ?: return false
            val position = if (fromEnd) siblings.size - index else index + 1
            return if (a == 0) position == b else (position - b) % a == 0 && (position - b) / a >= 0
        }
    }

    class Only(private val ofType: Boolean) : CssCondition {
        override fun matches(element: Element): Boolean =
            element.parent()?.children()?.count { !ofType || it.normalName() == element.normalName() }?.let { it == 1 }
                ?: true
    }

    data object Empty : CssCondition {
        override fun matches(element: Element): Boolean =
            element.childNodes().none { node -> node !is org.jsoup.nodes.Comment }
    }

    data object Root : CssCondition {
        override fun matches(element: Element): Boolean =
            element.parent()?.let { it is org.jsoup.nodes.Document } == true
    }

    /** `:not()`, `:is()` and `:where()` over a list of compound selectors. */
    class Logical(private val kind: String, private val alternatives: List<CssCompoundSelector>) : CssCondition {
        override val specificity: CssSpecificity
            get() = if (kind == "where") {
                CssSpecificity.ZERO
            } else {
                alternatives.maxOfOrNull { it.specificity }
                    ?: CssSpecificity.ZERO
            }

        override fun matches(element: Element): Boolean {
            val any = alternatives.any { it.matches(element) }
            return if (kind == "not") !any else any
        }
    }
}

private class SelectorReader(private val text: String) {
    private var position = 0
    val atEnd: Boolean get() = position >= text.length

    fun peek(): Char = text[position]

    fun next(): Char = text[position++]

    fun skipSpace(): Boolean {
        val start = position
        while (!atEnd && text[position].isWhitespace()) position++
        return position > start
    }

    fun compound(): CssCompoundSelector? {
        var tag: String? = null
        var id: String? = null
        val classes = mutableListOf<String>()
        val conditions = mutableListOf<CssCondition>()
        var any = false
        if (!atEnd && peek() == '*') {
            next()
            any = true
        } else if (!atEnd && peek().isNameStart()) {
            tag = name().lowercase()
            any = true
        }
        while (!atEnd) {
            when (peek()) {
                '#' -> {
                    next()
                    id = name().takeIf(String::isNotEmpty) ?: return null
                }
                '.' -> {
                    next()
                    classes += name().takeIf(String::isNotEmpty) ?: return null
                }
                '[' -> conditions += attribute() ?: return null
                ':' -> conditions += pseudoClass() ?: return null
                else -> break
            }
            any = true
        }
        return if (any) CssCompoundSelector(tag, id, classes, conditions) else null
    }

    private fun attribute(): CssCondition? {
        next() // [
        skipSpace()
        val name = name().lowercase().takeIf(String::isNotEmpty) ?: return null
        skipSpace()
        if (!atEnd && peek() == ']') {
            next()
            return CssCondition.Attribute(name, null, "", ignoreCase = false)
        }
        val operator = when {
            text.startsWith("=", position) -> "="
            text.length > position + 1 && text[position + 1] == '=' && text[position] in "~|^$*" ->
                text.substring(position, position + 2)
            else -> return null
        }
        position += operator.length
        skipSpace()
        val value = if (!atEnd && (peek() == '"' || peek() == '\'')) quoted() ?: return null else name()
        skipSpace()
        var ignoreCase = false
        if (!atEnd && peek().lowercaseChar() in "is") {
            ignoreCase = next().lowercaseChar() == 'i'
            skipSpace()
        }
        if (atEnd || next() != ']') return null
        return CssCondition.Attribute(name, operator, value, ignoreCase)
    }

    private fun pseudoClass(): CssCondition? {
        next() // :
        if (!atEnd && peek() == ':') return null // pseudo-element
        val name = name().lowercase()
        val argument = if (!atEnd && peek() == '(') parenthesized() ?: return null else null
        return when (name) {
            "first-child" -> CssCondition.Nth(0, 1, fromEnd = false, ofType = false)
            "last-child" -> CssCondition.Nth(0, 1, fromEnd = true, ofType = false)
            "first-of-type" -> CssCondition.Nth(0, 1, fromEnd = false, ofType = true)
            "last-of-type" -> CssCondition.Nth(0, 1, fromEnd = true, ofType = true)
            "only-child" -> CssCondition.Only(ofType = false)
            "only-of-type" -> CssCondition.Only(ofType = true)
            "nth-child", "nth-last-child", "nth-of-type", "nth-last-of-type" -> {
                val (a, b) = parseNth(argument ?: return null) ?: return null
                CssCondition.Nth(a, b, fromEnd = "last" in name, ofType = "type" in name)
            }
            "empty" -> CssCondition.Empty
            "root" -> CssCondition.Root
            "not", "is", "where", "matches" -> {
                val alternatives = argument?.split(',')?.map { alternative ->
                    SelectorReader(alternative.trim()).let { reader ->
                        reader.compound()?.takeIf { reader.atEnd } ?: return null
                    }
                } ?: return null
                CssCondition.Logical(if (name == "matches") "is" else name, alternatives)
            }
            // Link and form states never change in the reader: an unvisited link, enabled controls.
            "link", "any-link" -> CssCondition.Attribute("href", null, "", ignoreCase = false)
            else -> null
        }
    }

    private fun parseNth(raw: String): Pair<Int, Int>? {
        val value = raw.replace(" ", "").lowercase()
        return when (value) {
            "odd" -> 2 to 1
            "even" -> 2 to 0
            else -> if ('n' in value) {
                val a = when (val coefficient = value.substringBefore('n')) {
                    "", "+" -> 1
                    "-" -> -1
                    else -> coefficient.toIntOrNull() ?: return null
                }
                val b = value.substringAfter('n').takeIf(String::isNotEmpty)?.toIntOrNull() ?: 0
                a to b
            } else {
                0 to (value.toIntOrNull() ?: return null)
            }
        }
    }

    private fun parenthesized(): String? {
        next() // (
        val start = position
        var depth = 1
        while (!atEnd) {
            when (next()) {
                '(' -> depth++
                ')' -> if (--depth == 0) return text.substring(start, position - 1)
            }
        }
        return null
    }

    private fun quoted(): String? {
        val quote = next()
        val start = position
        while (!atEnd) {
            if (next() == quote) return text.substring(start, position - 1)
        }
        return null
    }

    private fun name(): String {
        val start = position
        while (!atEnd && (peek().isNameStart() || peek().isDigit() || peek() == '-')) {
            if (peek() == '\\' && position + 1 < text.length) position++
            position++
        }
        return text.substring(start, position).replace("\\", "")
    }

    private fun Char.isNameStart() = isLetter() || this == '_' || this == '-' || this == '\\' || code > 0x7F
}

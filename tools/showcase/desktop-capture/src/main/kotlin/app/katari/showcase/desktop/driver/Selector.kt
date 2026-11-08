package app.katari.showcase.desktop.driver

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import javax.accessibility.AccessibleRole

/** Which [Node] a scene means, described for failure messages. */
internal class Selector(private val description: String, private val predicate: (Node) -> Boolean) {
    fun matches(node: Node): Boolean = predicate(node)

    override fun toString(): String = description

    fun enabled() = Selector("$description, enabled") { matches(it) && it.enabled }

    companion object {
        fun text(text: String) = Selector("text \"$text\"") { text in it.texts }

        fun textStartsWith(prefix: String) = Selector("text starting \"$prefix\"") { node ->
            node.texts.any { it.startsWith(prefix) }
        }

        fun textMatches(pattern: Regex) = Selector("text matching $pattern") { node ->
            node.texts.any(pattern::matches)
        }

        fun description(description: String) = Selector("description \"$description\"") {
            description in it.contentDescriptions
        }

        fun tag(tag: String) = Selector("tag $tag") { it.testTag == tag }

        /** A button by its accessible name, which for a Compose button joins what it shows, such as "Personal, Active". */
        fun button(name: String) = Selector("button \"$name\"") {
            it.role == AccessibleRole.PUSH_BUTTON && it.name == name
        }

        /** A text field by its label, which Compose gives the field's node beside the text typed into it. */
        fun field(label: String) = Selector("field \"$label\"") { node ->
            node.semantics?.getOrNull(SemanticsProperties.EditableText) != null && label in node.texts
        }

        /** One of the navigation rail's or a screen's tabs. */
        fun tab(name: String) = Selector("tab \"$name\"") { it.role == AccessibleRole.PAGE_TAB && it.name == name }
    }
}

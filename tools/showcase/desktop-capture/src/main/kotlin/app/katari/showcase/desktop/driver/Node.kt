package app.katari.showcase.desktop.driver

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import java.awt.Rectangle
import java.awt.Window
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole

/**
 * One node of a window's accessibility tree as it was when read. A Compose node also carries its semantics: the
 * accessible name Compose gives a node is its content description when it has one, which hides the node's text, so
 * scenes match on the semantics instead. Swing's own dialogs, such as the file chooser, have names only.
 */
internal class Node(
    val window: Window,
    /** The nearest node above it in the tree that is on screen. */
    val parent: Node?,
    /** The node itself, for acting on it through its accessibility, as [Desktop.enterText] does. */
    val context: AccessibleContext,
    /** Where it is on screen, in the coordinates AWT's Robot takes. */
    val bounds: Rectangle,
    val role: AccessibleRole?,
    val name: String?,
    val texts: List<String>,
    val contentDescriptions: List<String>,
    val testTag: String?,
    val selected: Boolean,
    /** A Compose node's semantics, whose actions [Desktop] takes as assistive tools do; null for a Swing node. */
    val semantics: SemanticsConfiguration?,
) {
    override fun toString(): String = buildString {
        append(role).append(" name=").append(name)
        if (texts.isNotEmpty()) append(" text=").append(texts)
        if (contentDescriptions.isNotEmpty()) append(" description=").append(contentDescriptions)
        if (testTag != null) append(" tag=").append(testTag)
        if (selected) append(" selected")
        append(" @").append(bounds.x).append(',').append(bounds.y).append(' ')
        append(bounds.width).append('x').append(bounds.height)
    }
}

/** Every node of [window]'s tree that is on screen, in tree order. Reads AWT state, so it runs on the AWT thread. */
internal fun nodesOf(window: Window): List<Node> = buildList { collect(window, window, null, this) }

private fun collect(window: Window, accessible: Accessible, parent: Node?, nodes: MutableList<Node>) {
    val context = accessible.accessibleContext ?: return
    val node = screenBounds(context)?.let { bounds -> node(window, parent, accessible, context, bounds) }
    node?.let(nodes::add)
    for (index in 0 until context.accessibleChildrenCount) {
        context.getAccessibleChild(index)?.let { collect(window, it, node ?: parent, nodes) }
    }
}

private fun node(
    window: Window,
    parent: Node?,
    accessible: Accessible,
    context: AccessibleContext,
    bounds: Rectangle,
): Node {
    val semantics = semanticsOf(accessible)
    return Node(
        window = window,
        parent = parent,
        context = context,
        bounds = bounds,
        role = context.accessibleRole,
        name = context.accessibleName,
        texts = semantics?.let { config ->
            listOfNotNull(
                config.getOrNull(SemanticsProperties.Text)?.map { it.text },
                config.getOrNull(SemanticsProperties.EditableText)?.let { listOf(it.text) },
            ).flatten()
        }.orEmpty(),
        contentDescriptions = semantics?.getOrNull(SemanticsProperties.ContentDescription).orEmpty(),
        testTag = semantics?.getOrNull(SemanticsProperties.TestTag),
        selected = semantics?.getOrNull(SemanticsProperties.Selected) == true,
        semantics = semantics,
    )
}

private fun screenBounds(context: AccessibleContext): Rectangle? {
    val component = context.accessibleComponent ?: return null
    val location = component.locationOnScreen ?: return null
    val size = component.size ?: return null
    return Rectangle(location.x, location.y, size.width, size.height)
}

/**
 * The semantics behind a Compose node. ComposeAccessible, Compose's bridge from a semantics node to
 * javax.accessibility, exposes them through a getter its public API doesn't type, so it is found by name.
 */
private fun semanticsOf(accessible: Accessible): SemanticsConfiguration? {
    val getter = try {
        accessible.javaClass.getMethod("getSemanticsConfig")
    } catch (_: NoSuchMethodException) {
        return null
    }
    return getter.invoke(accessible) as? SemanticsConfiguration
}

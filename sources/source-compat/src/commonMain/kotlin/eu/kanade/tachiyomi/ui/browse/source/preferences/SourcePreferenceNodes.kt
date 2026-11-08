package eu.kanade.tachiyomi.ui.browse.source.preferences

/**
 * What one of a source's preferences shows, read from the androidx preference the source built. Each node's actions
 * route the change through that preference, so the source's listeners, dependencies and persistence apply as they did
 * in androidx's own screen; the tree has to be read again afterwards (see [SourcePreferenceHost.revision]).
 */
sealed interface SourcePreferenceNode {
    val title: String
    val summary: String?
    val enabled: Boolean

    data class Toggle(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val checked: Boolean,
        val toggle: () -> Unit,
    ) : SourcePreferenceNode

    /** [entries] maps each value to its label. */
    data class Choice(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val value: String?,
        val entries: Map<String, String>,
        val choose: (String) -> Unit,
    ) : SourcePreferenceNode

    /** [entries] maps each value to its label. */
    data class MultiChoice(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val values: Set<String>,
        val entries: Map<String, String>,
        val choose: (Set<String>) -> Unit,
    ) : SourcePreferenceNode

    /** [input] runs the source's bind listener, so it is called on every opening of the editor, as androidx does. */
    data class Text(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val dialogTitle: String,
        val dialogMessage: String?,
        val input: () -> SourceEditTextInput,
        val enter: (String) -> Unit,
    ) : SourcePreferenceNode

    data class Slider(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val value: Int,
        val range: IntProgression,
        val slide: (Int) -> Unit,
    ) : SourcePreferenceNode

    /**
     * Any other preference: a row that runs the preference's own click handling. [click] is null when the preference
     * can't be selected.
     */
    data class Action(
        override val title: String,
        override val summary: String?,
        override val enabled: Boolean,
        val click: (() -> Unit)?,
    ) : SourcePreferenceNode
}

/** A run of preferences under a category; a null [title] is a run outside any category. */
data class SourcePreferenceSection(
    val title: String?,
    val nodes: List<SourcePreferenceNode>,
)

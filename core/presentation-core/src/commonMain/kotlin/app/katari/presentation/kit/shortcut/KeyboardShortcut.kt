package app.katari.presentation.kit.shortcut

/**
 * What a keyboard shortcut is, apart from what it runs: [KeyboardShortcutHandler] binds the action where the owner of
 * the action is composed, and the shortcut list and tooltips read the rest. [id] tells two declarations of the same
 * shortcut apart from different ones, so the list shows it once. [keys] lists every way to trigger it; the first is
 * the one a tooltip names.
 *
 * [whileTyping] false leaves the keys to a focused text field, for keys typing may mean something to, such as Ctrl+F in
 * a search field or Ctrl+/ in a note.
 *
 * [beforeFocus] takes the keys before the focused control has them, for a screen whose keys mean the same wherever
 * focus is, such as a player's Space and arrows: they then never press the focused button or move focus.
 */
data class KeyboardShortcut(
    val id: String,
    val label: String,
    val keys: List<ShortcutKeys>,
    val group: ShortcutGroup,
    val reach: ShortcutReach = ShortcutReach.Screen,
    val whileTyping: Boolean = true,
    val beforeFocus: Boolean = false,
)

/** The heading a shortcut is listed under. */
enum class ShortcutGroup { Navigation, CurrentScreen, Window }

enum class ShortcutReach {
    /** Runs only while nothing covers what declared it, such as a dialog, sheet or menu opened over the screen. */
    Screen,

    /** Runs whatever is open in the window, such as going back or full screen. */
    Window,
}

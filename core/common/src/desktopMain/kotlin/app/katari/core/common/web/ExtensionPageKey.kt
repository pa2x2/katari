package app.katari.core.common.web

/**
 * A key of a keyboard as a page's scripts are told of it.
 *
 * @property key the event's `key`, such as `Enter`.
 * @property code its `code`, the physical key.
 * @property keyCode its legacy `keyCode`, which older scripts still read.
 * @property text what the key types, null for a key that types nothing.
 */
enum class ExtensionPageKey(val key: String, val code: String, val keyCode: Int, val text: String?) {
    ENTER("Enter", "Enter", 13, "\r"),
    TAB("Tab", "Tab", 9, null),
    ESCAPE("Escape", "Escape", 27, null),
    BACKSPACE("Backspace", "Backspace", 8, null),
    ARROW_UP("ArrowUp", "ArrowUp", 38, null),
    ARROW_DOWN("ArrowDown", "ArrowDown", 40, null),
    ARROW_LEFT("ArrowLeft", "ArrowLeft", 37, null),
    ARROW_RIGHT("ArrowRight", "ArrowRight", 39, null),
}

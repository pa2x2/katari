package app.katari.desktop.tray

import app.katari.desktop.dbus.Variant

/**
 * The tray icon's menu as `com.canonical.dbusmenu` describes it to the tray: a root (id 0) whose items open the app's
 * window and quit the app, with a separator between them. The menu never changes, so its layout has one revision.
 */
internal class TrayMenu(openLabel: String, quitLabel: String) {
    private val items: List<Item> = listOf(
        Item(OPEN_ID, standard(openLabel), TrayCommand.Open),
        Item(SEPARATOR_ID, mapOf("type" to Variant("s", "separator")), command = null),
        Item(QUIT_ID, standard(quitLabel), TrayCommand.Quit),
    )

    /**
     * The `(ia{sv}av)` layout under [parentId] with only [propertyNames] of each item, or every property when empty;
     * null for an id the menu doesn't have.
     */
    fun layout(parentId: Int, propertyNames: List<String>): List<Any?>? {
        if (parentId == ROOT_ID) {
            val children = items.map { Variant(LAYOUT_SIGNATURE, layoutOf(it, propertyNames)) }
            return listOf(ROOT_ID, ROOT_PROPERTIES.only(propertyNames), children)
        }
        return items.firstOrNull { it.id == parentId }?.let { layoutOf(it, propertyNames) }
    }

    /** The `a(ia{sv})` properties of [ids], or of every item when empty, leaving out ids the menu doesn't have. */
    fun groupProperties(ids: List<Int>, propertyNames: List<String>): List<List<Any?>> = (ids.ifEmpty { allIds })
        .mapNotNull { id -> propertiesOf(id)?.let { listOf(id, it.only(propertyNames)) } }

    /** Property [name] of item [id]; null where the item or the property doesn't exist. */
    fun property(id: Int, name: String): Variant? = propertiesOf(id)?.get(name)

    /** Whether the menu has an item [id], which the tray's events must name. */
    fun has(id: Int): Boolean = id == ROOT_ID || items.any { it.id == id }

    /** What clicking item [id] asks for; null for the root, the separator and ids the menu doesn't have. */
    fun commandOf(id: Int): TrayCommand? = items.firstOrNull { it.id == id }?.command

    private val allIds = listOf(ROOT_ID) + items.map { it.id }

    private fun propertiesOf(id: Int): Map<String, Variant>? =
        if (id == ROOT_ID) ROOT_PROPERTIES else items.firstOrNull { it.id == id }?.properties

    private fun layoutOf(item: Item, propertyNames: List<String>): List<Any?> =
        listOf(item.id, item.properties.only(propertyNames), emptyList<Variant>())

    private fun Map<String, Variant>.only(names: List<String>) = if (names.isEmpty()) {
        this
    } else {
        filterKeys {
            it in names
        }
    }

    private class Item(val id: Int, val properties: Map<String, Variant>, val command: TrayCommand?)

    companion object {
        const val LAYOUT_SIGNATURE = "(ia{sv}av)"
        const val REVISION = 1

        private const val ROOT_ID = 0
        private const val OPEN_ID = 1
        private const val SEPARATOR_ID = 2
        private const val QUIT_ID = 3

        private val ROOT_PROPERTIES = mapOf("children-display" to Variant("s", "submenu"))

        // An underscore marks the next letter as the item's access key, so a label's own underscores are doubled
        private fun standard(label: String) = mapOf("label" to Variant("s", label.replace("_", "__")))
    }
}

/** What the tray asks the app for: through its icon, to open the window, and through its menu, either. */
internal enum class TrayCommand {
    Open,
    Quit,
}

/**
 * [command] with the token the tray got from the desktop for the click that asked, which lets the window that answers
 * take the focus; null where the tray got none.
 */
internal data class TrayRequest(val command: TrayCommand, val activationToken: String?)

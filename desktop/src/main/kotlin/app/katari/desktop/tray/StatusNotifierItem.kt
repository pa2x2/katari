package app.katari.desktop.tray

import app.katari.desktop.application.icon.AppIcon
import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.DbusMessage
import app.katari.desktop.dbus.DbusStandardInterfaces
import app.katari.desktop.dbus.DbusStandardInterfaces.INTROSPECTABLE
import app.katari.desktop.dbus.DbusStandardInterfaces.INVALID_ARGS
import app.katari.desktop.dbus.DbusStandardInterfaces.PEER
import app.katari.desktop.dbus.DbusStandardInterfaces.PROPERTIES
import app.katari.desktop.dbus.DbusStandardInterfaces.SERVICE_UNKNOWN
import app.katari.desktop.dbus.DbusStandardInterfaces.UNKNOWN_INTERFACE
import app.katari.desktop.dbus.DbusStandardInterfaces.UNKNOWN_METHOD
import app.katari.desktop.dbus.DbusStandardInterfaces.UNKNOWN_OBJECT
import app.katari.desktop.dbus.Variant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.seconds

/**
 * Linux: an icon in the desktop's tray as StatusNotifierItem describes one, which KDE Plasma, GNOME's AppIndicator
 * extension, Cinnamon, Xfce, MATE and the bars of tiling compositors show, with [menu] as its menu through
 * `com.canonical.dbusmenu`. Clicking the icon, or an item of the menu, hands [onRequest] what was asked, on the item's
 * own thread.
 *
 * The session's StatusNotifierWatcher keeps the list of icons, and the trays (its hosts) read it, so the item follows
 * the watcher: it registers while [show]n and a watcher runs, again whenever a watcher starts, and lets go of its name
 * to leave the tray. It serves from [start] until [close] on a connection of its own.
 */
internal class StatusNotifierItem(
    private val id: String,
    private val title: String,
    private val menu: TrayMenu,
    private val onRequest: (TrayRequest) -> Unit,
) : AutoCloseable {
    private val host = MutableStateFlow(false)
    private val shownInTray = MutableStateFlow(false)

    /** Whether a tray that shows such icons runs in the session. */
    val hostAvailable: StateFlow<Boolean> = host.asStateFlow()

    /** Whether the icon is in a tray now: [show]n, registered with the session's watcher, and a tray runs. */
    val isShown: StateFlow<Boolean> = shownInTray.asStateFlow()

    // The name the watcher knows the item by, which the specification builds from the process and the item in it
    private val busName = "org.kde.StatusNotifierItem-${ProcessHandle.current().pid()}-1"

    private val pixmaps by lazy { ICON_SIZES.map { size -> listOf(size, size, bigEndian(AppIcon.argb(size))) } }

    @Volatile
    private var wanted = false

    @Volatile
    private var closed = false

    @Volatile
    private var bus: DbusConnection? = null

    // The serving thread's own state
    private var watcherRuns = false
    private var registrationTried = false
    private var ownsName = false
    private var registered = false
    private var activationToken: String? = null

    fun start() {
        Thread(::serve, "Tray").apply { isDaemon = true }.start()
    }

    /** Puts the icon in the tray or takes it out; cheap, as the serving thread does it. */
    fun show(show: Boolean) {
        wanted = show
        bus?.wakeUp()
    }

    override fun close() {
        closed = true
        bus?.wakeUp()
    }

    private fun serve() {
        val bus = DbusConnection.session() ?: return logcat(LogPriority.WARN) { "No session bus for the tray" }
        try {
            this.bus = bus
            bus.addMatch(WATCHER_OWNER_RULE)
            bus.addMatch(WATCHER_SIGNALS_RULE)
            readWatcher(bus)
            while (!closed) {
                follow(bus)
                shownInTray.value = registered && host.value
                val message = bus.nextMessage(IDLE_WAIT) ?: continue
                when (message.type) {
                    DbusMessage.SIGNAL -> onSignal(bus, message)
                    DbusMessage.METHOD_CALL -> answer(bus, message)
                }
            }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "The tray lost the session bus" }
        } catch (e: DbusException) {
            logcat(LogPriority.WARN) { "The session bus refused the tray: ${e.message}" }
        } finally {
            this.bus = null
            host.value = false
            shownInTray.value = false
            bus.close()
        }
    }

    /** Registers with the watcher or leaves the tray as [wanted] says, once per watcher. */
    private fun follow(bus: DbusConnection) {
        if (!wanted) {
            if (ownsName) {
                bus.releaseName(busName)
                ownsName = false
                registered = false
                registrationTried = false
            }
            return
        }
        if (!ownsName) ownsName = bus.requestName(busName, DbusConnection.NAME_FLAG_DO_NOT_QUEUE)
        if (ownsName && watcherRuns && !registrationTried) {
            registrationTried = true
            registered = try {
                bus.call(WATCHER, WATCHER_PATH, WATCHER_INTERFACE, "RegisterStatusNotifierItem", "s", busName)
                true
            } catch (e: DbusException) {
                logcat(LogPriority.WARN) { "The tray's watcher refused the app's icon: ${e.message}" }
                false
            }
        }
    }

    /** Looks for the watcher anew, which knows no icon that hasn't registered with it since it started. */
    private fun readWatcher(bus: DbusConnection) {
        registered = false
        registrationTried = false
        watcherRuns = readHost(bus)
    }

    /** Reads whether a tray runs into [host], and returns whether a watcher answered. */
    private fun readHost(bus: DbusConnection): Boolean {
        val answer = try {
            bus.call(
                WATCHER,
                WATCHER_PATH,
                PROPERTIES,
                "Get",
                "ss",
                WATCHER_INTERFACE,
                "IsStatusNotifierHostRegistered",
            )
        } catch (e: DbusException) {
            host.value = false
            return e.name != SERVICE_UNKNOWN
        }
        host.value = (answer.firstOrNull() as? Variant)?.value == true
        return true
    }

    private fun onSignal(bus: DbusConnection, signal: DbusMessage) {
        when {
            signal.member == "NameOwnerChanged" && signal.body.firstOrNull() == WATCHER -> readWatcher(bus)
            signal.iface != WATCHER_INTERFACE -> Unit
            signal.member == "StatusNotifierHostRegistered" -> host.value = true
            // Another tray may still run
            signal.member == "StatusNotifierHostUnregistered" -> readHost(bus)
        }
    }

    private fun answer(bus: DbusConnection, call: DbusMessage) {
        val member = call.member.orEmpty()
        val iface = call.iface ?: INTERFACE_OF_MEMBER[member]
        if (iface == PEER) return DbusStandardInterfaces.answerPeer(bus, call)
        when (call.path) {
            ITEM_PATH -> when (iface) {
                INTROSPECTABLE -> bus.reply(call, "s", STATUS_NOTIFIER_ITEM_INTROSPECTION)
                PROPERTIES -> DbusStandardInterfaces.answerProperties(bus, call) { name ->
                    itemProperties().takeIf { name == ITEM_INTERFACE }
                }
                ITEM_INTERFACE -> answerItem(bus, call, member)
                else -> bus.replyError(call, UNKNOWN_INTERFACE, "No interface $iface")
            }
            MENU_PATH -> when (iface) {
                INTROSPECTABLE -> bus.reply(call, "s", DBUSMENU_INTROSPECTION)
                PROPERTIES -> DbusStandardInterfaces.answerProperties(bus, call) { name ->
                    MENU_PROPERTIES.takeIf { name == MENU_INTERFACE }
                }
                MENU_INTERFACE -> answerMenu(bus, call, member)
                else -> bus.replyError(call, UNKNOWN_INTERFACE, "No interface $iface")
            }
            else -> bus.replyError(call, UNKNOWN_OBJECT, "No object at ${call.path}")
        }
    }

    private fun answerItem(bus: DbusConnection, call: DbusMessage, member: String) {
        when (member) {
            "Activate" -> request(TrayCommand.Open)
            // The token comes before the click it is for, which a later Activate or menu event answers
            "ProvideXdgActivationToken" -> activationToken = call.body.firstOrNull() as? String
            // The tray shows the menu itself from the Menu property; the rest asks for nothing the icon offers
            "ContextMenu", "SecondaryActivate", "Scroll" -> Unit
            else -> return bus.replyError(call, UNKNOWN_METHOD, "No method $member")
        }
        bus.reply(call)
    }

    private fun answerMenu(bus: DbusConnection, call: DbusMessage, member: String) {
        val body = call.body
        when (member) {
            "GetLayout" -> {
                val parentId = body.getOrNull(0) as? Int
                val layout = parentId?.let { menu.layout(it, body.strings(2)) }
                    ?: return bus.replyError(call, INVALID_ARGS, "No menu item $parentId")
                bus.reply(call, "u${TrayMenu.LAYOUT_SIGNATURE}", TrayMenu.REVISION, layout)
            }
            "GetGroupProperties" -> bus.reply(
                call,
                "a(ia{sv})",
                menu.groupProperties(body.ints(0), body.strings(1)),
            )
            "GetProperty" -> {
                val value =
                    (body.getOrNull(0) as? Int)?.let { id -> menu.property(id, body.getOrNull(1) as? String ?: "") }
                        ?: return bus.replyError(call, INVALID_ARGS, "No such menu item or property")
                bus.reply(call, "v", value)
            }
            "Event" -> {
                val itemId = body.getOrNull(0) as? Int
                if (itemId == null || !menu.has(itemId)) {
                    return bus.replyError(call, INVALID_ARGS, "No menu item $itemId")
                }
                onMenuEvent(itemId, body.getOrNull(1) as? String)
                bus.reply(call)
            }
            "EventGroup" -> {
                val unknown = (
                    body.getOrNull(
                        0,
                    ) as? List<*>
                    ).orEmpty().filterIsInstance<List<*>>().mapNotNull { event ->
                    val itemId = event.getOrNull(0) as? Int ?: return@mapNotNull null
                    if (!menu.has(itemId)) return@mapNotNull itemId
                    onMenuEvent(itemId, event.getOrNull(1) as? String)
                    null
                }
                bus.reply(call, "ai", unknown)
            }
            // The menu never changes, so it needs no update before it shows
            "AboutToShow" -> bus.reply(call, "b", false)
            "AboutToShowGroup" -> bus.reply(call, "aiai", emptyList<Int>(), emptyList<Int>())
            else -> bus.replyError(call, UNKNOWN_METHOD, "No method $member")
        }
    }

    private fun onMenuEvent(itemId: Int, eventId: String?) {
        if (eventId == "clicked") menu.commandOf(itemId)?.let(::request)
    }

    private fun request(command: TrayCommand) {
        onRequest(TrayRequest(command, activationToken))
        activationToken = null
    }

    private fun itemProperties(): Map<String, Variant> = mapOf(
        "Category" to Variant("s", "ApplicationStatus"),
        "Id" to Variant("s", id),
        "Title" to Variant("s", title),
        "Status" to Variant("s", "Active"),
        "WindowId" to Variant("i", 0),
        "IconThemePath" to Variant("s", ""),
        "IconName" to Variant("s", ""),
        "IconPixmap" to Variant(PIXMAPS, pixmaps),
        "OverlayIconName" to Variant("s", ""),
        "OverlayIconPixmap" to Variant(PIXMAPS, emptyList<Any>()),
        "AttentionIconName" to Variant("s", ""),
        "AttentionIconPixmap" to Variant(PIXMAPS, emptyList<Any>()),
        "AttentionMovieName" to Variant("s", ""),
        "ToolTip" to Variant("(s${PIXMAPS}ss)", listOf("", emptyList<Any>(), title, "")),
        "ItemIsMenu" to Variant("b", false),
        "Menu" to Variant("o", MENU_PATH),
    )

    private fun List<Any?>.strings(index: Int): List<String> =
        (getOrNull(index) as? List<*>).orEmpty().filterIsInstance<String>()

    private fun List<Any?>.ints(index: Int): List<Int> = (
        getOrNull(
            index,
        ) as? List<*>
        ).orEmpty().filterIsInstance<Int>()

    private companion object {
        const val WATCHER = "org.kde.StatusNotifierWatcher"
        const val WATCHER_PATH = "/StatusNotifierWatcher"
        const val WATCHER_INTERFACE = "org.kde.StatusNotifierWatcher"
        const val ITEM_PATH = "/StatusNotifierItem"
        const val ITEM_INTERFACE = "org.kde.StatusNotifierItem"
        const val MENU_PATH = "/MenuBar"
        const val MENU_INTERFACE = "com.canonical.dbusmenu"

        const val WATCHER_OWNER_RULE = "type='signal',sender='org.freedesktop.DBus',interface='org.freedesktop.DBus'," +
            "member='NameOwnerChanged',arg0='$WATCHER'"
        const val WATCHER_SIGNALS_RULE = "type='signal',interface='$WATCHER_INTERFACE'"

        // Pixmaps of the icon, each its width, its height and its ARGB pixels in network byte order
        const val PIXMAPS = "a(iiay)"

        // The sizes trays draw icons at, at the usual scales; each picks the closest and scales it
        val ICON_SIZES = listOf(16, 22, 24, 32, 48, 64)

        val MENU_PROPERTIES = mapOf(
            "Version" to Variant("u", 3),
            "TextDirection" to Variant("s", "ltr"),
            "Status" to Variant("s", "normal"),
            "IconThemePath" to Variant("as", emptyList<String>()),
        )

        // A call may leave out its interface; each member these objects answer belongs to one
        val INTERFACE_OF_MEMBER = DbusStandardInterfaces.INTERFACE_OF_MEMBER + buildMap {
            listOf("ContextMenu", "Activate", "SecondaryActivate", "Scroll", "ProvideXdgActivationToken")
                .forEach { put(it, ITEM_INTERFACE) }
            listOf(
                "GetLayout",
                "GetGroupProperties",
                "GetProperty",
                "Event",
                "EventGroup",
                "AboutToShow",
                "AboutToShowGroup",
            ).forEach { put(it, MENU_INTERFACE) }
        }

        // Updates and closing wake the thread sooner; this only bounds how long it sleeps without either
        val IDLE_WAIT = 5.seconds

        fun bigEndian(argb: IntArray): ByteArray {
            val bytes = ByteBuffer.allocate(argb.size * Int.SIZE_BYTES).order(ByteOrder.BIG_ENDIAN)
            bytes.asIntBuffer().put(argb)
            return bytes.array()
        }
    }
}

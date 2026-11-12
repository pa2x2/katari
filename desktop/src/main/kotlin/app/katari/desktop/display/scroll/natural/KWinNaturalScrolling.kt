package app.katari.desktop.display.scroll.natural

import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.Variant

/** KDE Plasma's compositor, KWin, which keeps the settings of each input device on an object of its own on D-Bus. */
internal object KWinNaturalScrolling {
    const val SERVICE = "org.kde.KWin"

    /**
     * Whether the enabled touchpads scroll naturally; null when there are none, or they differ. Throws what
     * [DbusConnection.call] throws.
     */
    fun read(bus: DbusConnection): Boolean? {
        val names = bus.call(SERVICE, DEVICES_PATH, PROPERTIES, "Get", "ss", MANAGER, "devicesSysNames")
            .first()
            .let { (it as Variant).value as List<*> }
        val settings = names.filterIsInstance<String>().mapNotNull { name ->
            val device = bus.call(
                SERVICE,
                "$DEVICES_PATH/$name",
                PROPERTIES,
                "GetAll",
                "s",
                DEVICE,
            ).first() as Map<*, *>
            fun flag(key: String) = (device[key] as? Variant)?.value as? Boolean
            if (flag("touchpad") == true && flag("enabled") == true) flag("naturalScroll") else null
        }
        return settings.distinct().singleOrNull()
    }

    private const val DEVICES_PATH = "/org/kde/KWin/InputDevice"
    private const val MANAGER = "org.kde.KWin.InputDeviceManager"
    private const val DEVICE = "org.kde.KWin.InputDevice"
    private const val PROPERTIES = "org.freedesktop.DBus.Properties"
}

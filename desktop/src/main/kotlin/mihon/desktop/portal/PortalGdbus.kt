package mihon.desktop.portal

import mihon.desktop.dbus.Gdbus

/** Linux: the XDG desktop portal, through [Gdbus]. */
internal object PortalGdbus : Gdbus(
    bus = Bus.Session,
    destination = "org.freedesktop.portal.Desktop",
    objectPath = "/org/freedesktop/portal/desktop",
)

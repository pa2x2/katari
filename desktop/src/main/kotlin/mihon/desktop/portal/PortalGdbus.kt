package mihon.desktop.portal

import mihon.desktop.dbus.Gdbus

/** Linux: the XDG desktop portal, through [Gdbus]. */
internal object PortalGdbus : Gdbus(
    bus = Bus.Session,
    destination = DesktopPortal.SERVICE,
    objectPath = DesktopPortal.PATH,
)

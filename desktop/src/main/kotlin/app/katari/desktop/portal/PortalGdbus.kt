package app.katari.desktop.portal

import app.katari.desktop.dbus.Gdbus

/** Linux: the XDG desktop portal, through [Gdbus]. */
internal object PortalGdbus : Gdbus(
    bus = Bus.Session,
    destination = DesktopPortal.SERVICE,
    objectPath = DesktopPortal.PATH,
)

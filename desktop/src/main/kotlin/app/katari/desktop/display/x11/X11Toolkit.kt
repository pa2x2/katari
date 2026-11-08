package app.katari.desktop.display.x11

import java.awt.Toolkit

/** Whether AWT runs on its X11 toolkit. Starts the toolkit, so it runs only once the window toolkit is chosen. */
internal fun isX11Toolkit(): Boolean = Toolkit.getDefaultToolkit().javaClass.name == "sun.awt.X11.XToolkit"

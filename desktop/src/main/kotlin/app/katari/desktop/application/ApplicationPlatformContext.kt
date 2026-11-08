package app.katari.desktop.application

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject

/** The application as a whole, for calls that come from no window, such as the messages a view model builds. */
@Inject
class ApplicationPlatformContext : PlatformContext()

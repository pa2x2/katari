package mihon.desktop.application

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext

/** The application as a whole, for calls that come from no window, such as the messages a view model builds. */
@Inject
class ApplicationPlatformContext : PlatformContext()

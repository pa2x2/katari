package mihon.desktop.application.instance

import kotlinx.serialization.Serializable

/**
 * What a later launch of the app asks of the running instance, which answers every request by bringing its window
 * forward and opening what the launch opens. Every field needs a default, so an instance still reads the requests of a
 * launch from an older or newer version.
 */
@Serializable
data class InstanceRequest(
    /**
     * The token the desktop gave the later launch to hand the focus on with, an XDG activation token or an X11 startup
     * id; null when it was started without one, such as from a terminal. Only the display backend reads it.
     */
    val activationToken: String? = null,
    /**
     * The links and files the launch opens, in order, as absolute URIs (see
     * [mihon.desktop.application.launch.LaunchArguments]). An instance opens those it knows and logs the others, which
     * a newer launch may send.
     */
    val links: List<String> = emptyList(),
)

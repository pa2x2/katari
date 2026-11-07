package mihon.app.sync

import android.app.Application
import android.os.Build
import android.provider.Settings
import app.katari.sync.client.connection.ThisDevice
import app.katari.sync.protocol.device.DeviceDescription
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.about.versionText
import mihon.app.ui.buildinfo.AppBuild

/**
 * The name the person gave the device in its settings, or its maker and model where it has none, with the app's version
 * as About names it, so two builds of the app on one phone (a debug and a release one) tell apart in the device list.
 */
@Inject
class AndroidThisDevice(
    private val application: Application,
    private val build: AppBuild,
) : ThisDevice {
    override fun description(): DeviceDescription {
        val named = Settings.Global.getString(application.contentResolver, Settings.Global.DEVICE_NAME)
        val model = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
            Build.MODEL
        } else {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }
        return DeviceDescription(
            name = named?.takeIf(String::isNotBlank) ?: model,
            platform = "Android",
            appVersion = appVersion,
        )
    }

    override val appVersion: String get() = build.versionText()
}

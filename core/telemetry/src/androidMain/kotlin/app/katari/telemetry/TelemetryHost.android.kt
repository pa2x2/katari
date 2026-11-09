package app.katari.telemetry

import android.app.Application

actual class TelemetryHost(val application: Application) {
    actual val isKatariRelease: Boolean
        get() = application.packageName == KATARI_PACKAGE
}

private const val KATARI_PACKAGE = "app.katari"

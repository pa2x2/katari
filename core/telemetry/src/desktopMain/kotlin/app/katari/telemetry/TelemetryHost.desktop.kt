package app.katari.telemetry

/** [appVersion] is the desktop app's own version, which nothing on the JVM reports for it. */
actual class TelemetryHost(val appVersion: String)

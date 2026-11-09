package app.katari.telemetry

/** What telemetry starts from on each platform: the Android app itself, or what the desktop app says of its build. */
expect class TelemetryHost {
    /**
     * Whether this is Katari's own release. Forks and Katari's other builds, such as the development and preview ones,
     * send nothing, as they would mix into the release's data.
     */
    val isKatariRelease: Boolean
}

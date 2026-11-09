package app.katari.telemetry

/** Katari's PostHog project, which both apps send to. The token only lets events in, so it can be public. */
internal object PostHogProject {
    const val TOKEN = "phc_wstiT9qNWK9UMh4WPc4qogVN2nuYm39ivvKAaxWsEyrD"
    const val HOST = "https://eu.i.posthog.com"
}

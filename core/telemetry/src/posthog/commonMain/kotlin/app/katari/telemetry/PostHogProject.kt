package app.katari.telemetry

/**
 * Katari's PostHog project, which both apps send to. Its token comes from the build, in [PostHogProjectToken], so that
 * only Katari's releases send to it.
 */
internal object PostHogProject {
    const val HOST = "https://eu.i.posthog.com"
}

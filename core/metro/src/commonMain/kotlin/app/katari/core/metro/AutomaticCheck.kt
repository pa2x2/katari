package app.katari.core.metro

/**
 * A look for something newer to offer, such as an update, that the app makes by itself while it is in use: the first
 * time it is in use after the process starts, then again every few hours for as long as it stays in use. Not at each
 * launch, which a process kept running for days, as instant sync keeps Android's, would rarely see. Contribute it with
 * `@ContributesIntoSet`.
 */
fun interface AutomaticCheck {
    /**
     * Returns false when the check couldn't be made, such as before the network is up, so it is tried again soon; one
     * turned off in settings has nothing to make and returns true.
     */
    suspend fun check(): Boolean
}

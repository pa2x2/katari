package app.katari.sync.client.run

/**
 * What triggers ask of a sync run. Requests made while a run is going are merged into the next one, so a burst of
 * triggers costs one run after the current one.
 */
data class SyncRequest(
    /** Entries to fetch the changes of ahead of everything else (D052). */
    val entriesFirst: Set<Long> = emptySet(),
    /**
     * Only send what changed here, and skip the server altogether when nothing did, as after a local edit; the run
     * still pulls first when it does go, so what it pushes is based on the newest copies.
     */
    val onlyIfChanged: Boolean = false,
) {
    fun merge(other: SyncRequest) = SyncRequest(
        entriesFirst = entriesFirst + other.entriesFirst,
        onlyIfChanged = onlyIfChanged && other.onlyIfChanged,
    )
}

/** How a sync run ended. */
enum class SyncOutcome {
    /** Pulled and pushed everything, or had nothing to send. */
    SYNCED,

    /** Not set up, stopped, paused, or waiting for the person's first-sync choices: nothing to run. */
    IDLE,

    /** No network. */
    OFFLINE,

    /** A network, but the server didn't answer, or not as a sync server. */
    UNREACHABLE,

    /**
     * A first download waits for Wi-Fi, which the person didn't allow to use mobile data, or changes do, which the
     * person keeps off mobile data.
     */
    WAITING_FOR_WIFI,

    /** Something went wrong that retrying later may fix, such as a write that failed. */
    FAILED,
}

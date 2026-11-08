package app.katari.sync.client.connection

/**
 * Where this device's connection to its account has got, kept in `sync_state.stage` so that a first sync or a full
 * resync interrupted anywhere carries on from where it stopped.
 *
 * A device joining an account goes [JOINING], [CHOOSING] when it has to ask, [RECORDING], [DOWNLOADING], [SETTINGS]
 * and then [READY] ([app.katari.sync.client.firstsync.FirstSyncSteps]); one that synced with the account before goes
 * [RECORDING_KEPT] and [RESYNC] after the choices instead. A device that finds its server lost or restored goes back
 * through [RESYNC] or [RESYNC_WITHOUT_DELETIONS] ([app.katari.sync.client.resync.FullResync]).
 */
enum class SyncStage(internal val stored: String) {
    /** Registered and capturing; the account's profiles are compared with this device's next. */
    JOINING("joining"),

    /** Waits for the person to say which of this device's profiles are which of the account's. */
    CHOOSING("choosing"),

    /** Records the rows this device held before sync started, so they merge with the account's. */
    RECORDING("recording"),

    /**
     * As [RECORDING], for a device that synced with this account before and kept its records (D066): only the rows they
     * don't stand for are recorded as new, then the device resyncs ([RESYNC]).
     */
    RECORDING_KEPT("recording-kept"),

    /** Takes the account's records, a page at a time. */
    DOWNLOADING("downloading"),

    /** Records the preferences this device held before sync started. */
    SETTINGS("settings"),

    /** Pulls the whole account again and pushes what the server lacks: the server was lost or restored. */
    RESYNC("resync"),

    /**
     * As [RESYNC], for a server that no longer keeps the deletions since the device's position: records the device
     * synced that the server no longer has were deleted elsewhere.
     */
    RESYNC_WITHOUT_DELETIONS("resync-without-deletions"),

    READY("ready"),
    ;

    /** Whether the device takes everything from the account now, which pushes wait for. */
    val isDownloading: Boolean get() = this == DOWNLOADING || this == RESYNC || this == RESYNC_WITHOUT_DELETIONS

    /** Whether the device is in its first sync with the account, from recording what it held to its settings. */
    val isFirstDownload: Boolean
        get() = this == RECORDING || this == RECORDING_KEPT || this == DOWNLOADING || this == SETTINGS

    /**
     * Whether the device moves its whole library to or from the account now: in its first sync, or a full resync. The
     * run that carries it on also uploads the library once it is ready, often for minutes: the long run whose progress
     * the platform shows and which it keeps going in the background (D007).
     */
    val movesLibrary: Boolean
        get() = isFirstDownload || this == RESYNC || this == RESYNC_WITHOUT_DELETIONS

    internal companion object {
        fun of(stored: String): SyncStage = entries.firstOrNull { it.stored == stored } ?: READY
    }
}

/** Why this device doesn't sync now although it was set up, kept in `sync_state.stopped`. */
enum class SyncStop(internal val stored: String) {
    /**
     * The server didn't take the device's token: it was replaced by a new empty one, or restored from a copy older
     * than the device's sign-in. The device keeps capturing, and signing in again resyncs everything it holds.
     */
    SIGN_IN_AGAIN("sign-in-again"),

    /**
     * As [SIGN_IN_AGAIN], from a server whose storage started over (another instance), such as one replaced by an
     * empty one: the account the device signed in to is gone and is made again on the server before signing in (D022).
     */
    SERVER_RESET("server-reset"),

    /** Removed from the account on another device or the web page; standalone again. */
    REMOVED("removed"),

    /** The account was disabled on the server; standalone again. */
    ACCOUNT_DISABLED("account-disabled"),

    /** The server only speaks newer protocol versions; syncing resumes once the app is updated. */
    APP_TOO_OLD("app-too-old"),

    /** The server only speaks older protocol versions; syncing resumes once the server is updated. */
    SERVER_TOO_OLD("server-too-old"),
    ;

    internal companion object {
        fun of(stored: String?): SyncStop? = entries.firstOrNull { it.stored == stored }
    }
}

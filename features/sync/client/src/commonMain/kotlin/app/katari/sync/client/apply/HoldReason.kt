package app.katari.sync.client.apply

/**
 * Why a pulled record waits in `sync_pending` instead of being applied, as the table stores it. A record that
 * [waitsForUpdate] is tried again only once the app version differs from the one that held it.
 */
internal enum class HoldReason(val stored: String, val waitsForUpdate: Boolean = false) {
    /** A kind, or a version of one, this app can't read; applied once an update can. */
    UNREADABLE("unreadable", waitsForUpdate = true),

    /** Edited more than the clock bound ahead of this device's clock; applied once the clock is within it. */
    AHEAD("ahead"),

    /** A record it refers to isn't here yet; applied when that record is. */
    PARENT("parent"),

    /** A preference that names a source or record this device can't name yet; tried again with each sync. */
    REFERENCE("reference"),

    /** The deletion of the profile open on this device; applied once another profile is open. */
    PROFILE_OPEN("profile-open"),

    /**
     * Applying it threw, as a bug in this app version or data it can't take does (D067); tried again once the app is
     * updated, and meanwhile neither pushed nor stopping the records around it.
     */
    FAILED("failed", waitsForUpdate = true),
}

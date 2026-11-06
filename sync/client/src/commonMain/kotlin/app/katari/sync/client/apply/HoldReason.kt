package app.katari.sync.client.apply

/** Why a pulled record waits in `sync_pending` instead of being applied, as the table stores it. */
internal enum class HoldReason(val stored: String) {
    /** A kind, or a version of one, this app can't read; applied once an update can. */
    UNREADABLE("unreadable"),

    /** Edited more than the clock bound ahead of this device's clock; applied once the clock is within it. */
    AHEAD("ahead"),

    /** A record it refers to isn't here yet; applied when that record is. */
    PARENT("parent"),

    /** A preference that names a source or record this device can't name yet; tried again with each sync. */
    REFERENCE("reference"),

    /** The deletion of the profile open on this device; applied once another profile is open. */
    PROFILE_OPEN("profile-open"),
}

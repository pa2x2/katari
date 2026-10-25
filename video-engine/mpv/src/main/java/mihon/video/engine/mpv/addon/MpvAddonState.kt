package mihon.video.engine.mpv.addon

/** The add-on as installed on the device, which is the same for every profile. */
sealed interface MpvAddonState {
    /** Not read yet in this process. */
    data object Unknown : MpvAddonState

    data object NotInstalled : MpvAddonState

    data class Installed(
        val versionName: String,
        val versionCode: Long,
        /** Null when the add-on declares no glue level. */
        val glue: Int?,
        /** The APK and its extracted libraries. */
        val sizeBytes: Long,
        /** Null when Katari can load it. */
        val problem: MpvAddonProblem?,
        /**
         * This process already runs another version's libraries, which stay loaded until Katari restarts. That
         * version keeps working, but this one takes effect only after the restart.
         */
        val restartNeeded: Boolean,
    ) : MpvAddonState
}

enum class MpvAddonProblem {
    /** Signed with another key than Katari, so it can't be trusted and Android won't update it over. */
    ForeignSigner,

    /** Built against another `MPVLib` than this Katari's: needs the add-on release for this Katari. */
    GlueMismatch,

    /** Older than the libmpv release this Katari was built with. */
    TooOld,

    /** Its libraries are for another ABI than Katari runs as. */
    WrongAbi,

    /** It has no load order or no libraries for Katari's ABI. */
    Damaged,
}

/** Updating the add-on, as opposed to removing it, makes it usable. */
val MpvAddonProblem.isFixedByUpdate: Boolean
    get() = this != MpvAddonProblem.ForeignSigner

package mihon.feature.profiles.ui

import dev.icerock.moko.resources.StringResource

/** The lines a platform's lock settings carry under their titles (see [ProfileUnlock.lockSettingsWording]). */
class ProfileLockWording(
    /** Under "Require unlock": what the unlock asks for. */
    val requireUnlock: StringResource,
    /** Under "Lock when idle", formatted with the chosen time: when that time counts. */
    val lockWhenIdle: StringResource,
    /** Where the platform can't ask, in place of the lock settings: why profiles can't be locked. */
    val unavailable: StringResource,
)

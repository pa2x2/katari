package mihon.feature.profiles.core

import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

/** Feature-owned view of every profile preference owner discovered during ordinary runtime installation. */
class ProfilePreferenceOwnership(
    private val owners: ProfilePreferenceOwnerRegistry,
) {
    data class Keys(
        val profile: Set<String>,
        val appState: Set<String>,
        val private: Set<String>,
    )

    fun derive(
        existingKeys: Set<String>,
        group: ProfilePreferenceOwnerGroupId? = null,
    ): Keys {
        val ownership = owners.ownership(existingKeys, group)
        return Keys(
            profile = ownership.profileKeys,
            appState = ownership.appStateKeys,
            private = ownership.privateKeys,
        )
    }
}

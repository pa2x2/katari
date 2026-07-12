package mihon.feature.profiles.core

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: Long,
    val uuid: String,
    val name: String,
    val colorSeed: Long,
    val position: Long,
    val requiresAuth: Boolean,
    val isArchived: Boolean,
)

internal fun List<Profile>.hasNameConflict(
    name: String,
    excludedProfileId: Long? = null,
): Boolean {
    val normalizedName = name.trim()
    if (normalizedName.isEmpty()) return false

    return any { profile ->
        profile.id != excludedProfileId &&
            profile.name.trim().equals(normalizedName, ignoreCase = true)
    }
}

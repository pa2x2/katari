package app.katari.profiles.core

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

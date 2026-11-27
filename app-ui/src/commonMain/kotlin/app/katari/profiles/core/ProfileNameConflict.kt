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

/** [name], or the first of "[name] 2", "[name] 3" and on that no profile has. */
internal fun List<Profile>.unusedName(name: String): String =
    (sequenceOf(name) + generateSequence(2) { it + 1 }.map { "$name $it" }).first { !hasNameConflict(it) }

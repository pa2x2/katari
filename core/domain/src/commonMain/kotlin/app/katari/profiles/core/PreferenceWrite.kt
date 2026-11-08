package app.katari.profiles.core

/**
 * A preference written: [key] as preference classes ask for it, app-state and private prefixes included, in
 * [profileId]'s own stores, or in the store all profiles share when [profileId] is null.
 */
data class PreferenceWrite(val profileId: Long?, val key: String)

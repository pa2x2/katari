package app.katari.domain.profile

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions

/**
 * The person-level preferences the app writes into a profile itself, before the person does anything: into the
 * default profile on the first start, and into each new profile. Sync's first sync asks what to do with a device's
 * data only when there is some (D047), so a value that is still what the app seeded is not the person's.
 */
object ProfileSeedPreferences {
    /** Seeds [store]'s preferences that aren't set yet. */
    fun seed(store: PreferenceStore) {
        seeds(store).forEach(Seed<*>::seed)
    }

    /**
     * The keys of [store] that hold the value the app seeded, unchanged since, as the app names them: a platform's store
     * may give a preference a key of its own, as Android's does one that carries the profile.
     */
    fun untouched(store: PreferenceStore): Set<String> =
        seeds(store).filter(Seed<*>::isUntouched).mapTo(HashSet()) { it.key }

    private fun seeds(store: PreferenceStore): List<Seed<*>> = listOf(
        Seed(
            DuplicatePreferences.TITLE_EXCLUSION_PATTERNS_KEY,
            DuplicatePreferences(store).titleExclusionPatterns,
            DuplicateTitleExclusions.defaultPatterns,
        ),
    )

    private class Seed<T>(val key: String, private val preference: Preference<T>, private val value: T) {
        fun seed() {
            if (!preference.isSet()) preference.set(value)
        }

        fun isUntouched() = preference.isSet() && preference.get() == value
    }
}

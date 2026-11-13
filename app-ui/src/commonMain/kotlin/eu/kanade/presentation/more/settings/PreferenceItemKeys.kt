package eu.kanade.presentation.more.settings

/**
 * Keys for a preference list's items from what each one is rather than where it is, so what a row keeps while shown,
 * such as its open sync popover, stays with that row when rows above it come or go, such as a hint atop the page. A
 * row that stores a setting is keyed by the setting, any other by its title; alike ones count up in their order. Ask
 * for every item of one list, in order, from one instance made for that pass over the list.
 */
internal class PreferenceItemKeys {
    private val counts = HashMap<String, Int>()

    fun of(preference: Preference): String {
        val base = when (preference) {
            is Preference.PreferenceGroup -> "group:${preference.title}"
            is Preference.PreferenceItem<*, *> ->
                preference.storedPreference?.let { "setting:${it.key()}" } ?: "row:${preference.title}"
        }
        val count = (counts[base] ?: 0) + 1
        counts[base] = count
        return if (count == 1) base else "$base#$count"
    }
}

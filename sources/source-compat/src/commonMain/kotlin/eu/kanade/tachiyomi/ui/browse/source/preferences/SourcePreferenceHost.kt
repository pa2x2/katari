package eu.kanade.tachiyomi.ui.browse.source.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference
import app.katari.entry.interactions.source.EntrySourceSettingsPopulateResult
import app.katari.entry.interactions.source.EntrySourceSettingsResolution
import eu.kanade.tachiyomi.data.preference.SharedPreferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Hosts the androidx preference hierarchy a source builds, without androidx's fragment. Extensions are compiled against
 * `setupPreferenceScreen(PreferenceScreen)`, so the source still gets a real screen backed by its own preferences; the
 * app only reads that tree into [SourcePreferenceNode]s and routes each change through it the way androidx's dialogs
 * and click handling do: the change listener first, which may veto it, then the preference's own setter, which
 * persists it and updates its dependents.
 *
 * The only way to own a [PreferenceManager] without `PreferenceFragmentCompat` is its library-group constructor; nothing
 * public creates one outside the fragment.
 *
 * @param context what the source builds its preferences with and their intents start from.
 */
class SourcePreferenceHost(
    private val context: Context,
    settings: EntrySourceSettingsResolution.Available,
) {
    private val preferences = settings.preferences

    @SuppressLint("RestrictedApi")
    private val manager = PreferenceManager(context).apply {
        preferenceDataStore = SharedPreferencesDataStore(preferences)
    }
    private val screen = manager.createPreferenceScreen(context)

    private val changes = MutableStateFlow(0)

    /** Moves on every change, so readers take fresh [sections] when a listener edits other preferences too. */
    val revision: StateFlow<Int> = changes.asStateFlow()

    private val onStoredChange = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        changes.update { it + 1 }
    }

    init {
        when (val result = settings.populate(screen)) {
            EntrySourceSettingsPopulateResult.Populated -> Unit
            is EntrySourceSettingsPopulateResult.Failed ->
                logcat(LogPriority.ERROR, result.cause) { "Failed to populate source preferences" }
        }
        // Attaching registers dependencies, which look their targets up through the manager's screen.
        manager.setPreferences(screen)
        screen.onAttached()
        preferences.registerOnSharedPreferenceChangeListener(onStoredChange)
    }

    fun sections(): List<SourcePreferenceSection> = sourcePreferenceSections(screen, this)

    fun release() {
        preferences.unregisterOnSharedPreferenceChangeListener(onStoredChange)
        screen.onDetached()
    }

    /** What `performClick()` does for a switch: the change listener may veto the flip, then the click listener runs. */
    internal fun toggle(preference: TwoStatePreference) = change {
        if (!preference.isEnabled || !preference.isSelectable) return@change
        val checked = !preference.isChecked
        if (preference.callChangeListener(checked)) preference.isChecked = checked
        preference.onPreferenceClickListener?.onPreferenceClick(preference)
    }

    /** What `performClick()` does for a plain preference: its click listener, else its intent. */
    internal fun click(preference: Preference) = change {
        if (!preference.isEnabled || !preference.isSelectable) return@change
        if (preference.onPreferenceClickListener?.onPreferenceClick(preference) == true) return@change
        preference.intent?.let(context::startActivity)
    }

    internal fun choose(preference: ListPreference, value: String) = change {
        if (preference.callChangeListener(value)) preference.value = value
    }

    internal fun choose(preference: MultiSelectListPreference, values: Set<String>) = change {
        if (preference.callChangeListener(values)) preference.values = values
    }

    internal fun editTextInput(preference: EditTextPreference): SourceEditTextInput = preference.editTextInput(context)

    internal fun enter(preference: EditTextPreference, text: String) = change {
        if (preference.callChangeListener(text)) preference.text = text
    }

    internal fun slide(preference: SeekBarPreference, value: Int) = change {
        if (preference.callChangeListener(value)) preference.value = value
    }

    private inline fun change(block: () -> Unit) {
        block()
        changes.update { it + 1 }
    }
}

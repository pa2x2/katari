package app.katari.sync.ui.arrival.change.named

import androidx.compose.runtime.Composable
import app.katari.profiles.core.ProfileStore
import app.katari.sync.ui.arrival.change.ChangePart
import app.katari.sync.ui.arrival.change.ChangedValue
import app.katari.sync.ui.arrival.change.KeyChange
import app.katari.sync.ui.arrival.change.setChanges
import app.katari.sync.ui.arrival.change.switchChange
import app.katari.sync.ui.arrival.change.valueChange
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.StoredValuesPreferenceStore
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A synced setting no Settings row stores, changed where it is used, such as the player's time label or the migration
 * options: [name] is what that place calls the setting with [key][matches], and [changes] tells what changed in it.
 */
internal class NamedSetting(
    val matches: (key: String) -> Boolean,
    val name: @Composable (key: String) -> String,
    val changes: @Composable (KeyChange) -> List<ChangePart>?,
)

/**
 * A setting read through [preference] the way the app reads it, named [name] and told by [tell], which is given the
 * profile whose store holds it, or null for the store all profiles share.
 */
internal fun <T> namedSetting(
    name: StringResource,
    preference: (PreferenceStore) -> Preference<T>,
    tell: @Composable (name: String, value: ChangedValue<T>, profileId: Long?) -> List<ChangePart>,
): NamedSetting {
    val key = preference(StoredValuesPreferenceStore(emptyMap())).key()
    return NamedSetting(
        matches = { it == key },
        name = { stringResource(name) },
        changes = { change -> change.of(preference)?.let { tell(stringResource(name), it, change.profileId) } },
    )
}

/**
 * The settings of a key family, one key for each item its suffix names ([item], null for a key it doesn't name), such
 * as a source's display mode, each named by [name] and told by [tell].
 */
internal fun <I : Any, T> familySetting(
    prefix: String,
    item: (suffix: String) -> I?,
    name: @Composable (item: I) -> String,
    preference: (item: I) -> (PreferenceStore) -> Preference<T>,
    tell: @Composable (name: String, value: ChangedValue<T>) -> List<ChangePart>,
): NamedSetting {
    fun itemOf(key: String): I? = key.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.let(item)
    return NamedSetting(
        matches = { itemOf(it) != null },
        name = { key -> name(checkNotNull(itemOf(key))) },
        changes = { change ->
            val of = checkNotNull(itemOf(change.key))
            change.of(preference(of))?.let { tell(name(of), it) }
        },
    )
}

/** An on/off setting: "Turned on". */
internal fun switchSetting(name: StringResource, preference: (PreferenceStore) -> Preference<Boolean>): NamedSetting =
    namedSetting(name, preference) { text, value, _ -> listOfNotNull(switchChange(text, value.before, value.after)) }

/** A setting with one value of several, each told by [label]: "System → Dark". */
internal fun <T> valueSetting(
    name: StringResource,
    preference: (PreferenceStore) -> Preference<T>,
    label: @Composable (T) -> String,
): NamedSetting = namedSetting(name, preference) { text, value, _ ->
    listOfNotNull(valueChange(text, label(value.before), label(value.after)))
}

/** A set of items, each told by [label]: "Added Updates · Removed History". */
internal fun <T> itemsSetting(
    name: StringResource,
    preference: (PreferenceStore) -> Preference<Set<T>>,
    label: @Composable (T) -> String,
): NamedSetting = namedSetting(name, preference) { text, value, _ ->
    setChanges(text, value.before, value.after, label)
}

/**
 * An ordered list of items, each told by [label]: what was added and taken out, or that only the order changed, with the
 * order before and after in full.
 */
internal fun <T> orderSetting(
    name: StringResource,
    preference: (PreferenceStore) -> Preference<List<T>>,
    label: @Composable (T) -> String,
): NamedSetting = namedSetting(name, preference) { text, value, _ ->
    val (before, after) = value
    setChanges(text, before.toSet(), after.toSet(), label).ifEmpty {
        val orders =
            valueChange(text, before.map { label(it) }.joinToString(", "), after.map { label(it) }.joinToString(", "))
        listOfNotNull(
            orders?.let {
                ChangePart(text, stringResource(MR.strings.sync_arrived_reordered), it.full, readsAlone = false)
            },
        )
    }
}

/**
 * The preference [bound] is, for one profile or the shared store, read from any store: a primitive setting's class
 * asks a store for it by its key and default alone, so this reads it as that class does. For a setting whose class
 * this module can't reach, such as the speech rate behind [app.katari.tts.api.host.TtsHostActions].
 */
internal fun ProfileStore.floatPreference(bound: Preference<Float>): (PreferenceStore) -> Preference<Float> {
    val key = preferenceKey(bound)
    return { store -> store.getFloat(key, bound.defaultValue()) }
}

internal fun ProfileStore.intPreference(bound: Preference<Int>): (PreferenceStore) -> Preference<Int> {
    val key = preferenceKey(bound)
    return { store -> store.getInt(key, bound.defaultValue()) }
}

internal fun ProfileStore.booleanPreference(bound: Preference<Boolean>): (PreferenceStore) -> Preference<Boolean> {
    val key = preferenceKey(bound)
    return { store -> store.getBoolean(key, bound.defaultValue()) }
}

/** The preference [bound] is, kept in a store as text through [serializer] and [deserializer], read from any store. */
internal fun <T> ProfileStore.objectPreference(
    bound: Preference<T>,
    serializer: (T) -> String,
    deserializer: (String) -> T,
): (PreferenceStore) -> Preference<T> {
    val key = preferenceKey(bound)
    return { store -> store.getObjectFromString(key, bound.defaultValue(), serializer, deserializer) }
}

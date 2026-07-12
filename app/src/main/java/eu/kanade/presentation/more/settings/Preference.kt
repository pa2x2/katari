package eu.kanade.presentation.more.settings

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.data.track.Tracker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import mihon.feature.profiles.core.ProfileAwarePreferenceStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.core.common.preference.Preference as CorePreference
import tachiyomi.core.common.preference.Preference as PreferenceData

sealed class Preference {
    abstract val title: String
    abstract val enabled: Boolean

    sealed class PreferenceItem<T, R> : Preference() {
        abstract val subtitle: String?
        abstract val icon: ImageVector?
        open val isProfileSpecific: Boolean = false
        abstract val onValueChanged: suspend (value: T) -> R

        /**
         * A basic [PreferenceItem] that only displays texts.
         */
        data class TextPreference(
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val isProfileSpecific: Boolean = false,
            val widget: @Composable (() -> Unit)? = null,
            val onClick: (() -> Unit)? = null,
        ) : PreferenceItem<String, Unit>() {
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: String) -> Unit = {}
        }

        /**
         * A [PreferenceItem] that provides a two-state toggleable option.
         */
        data class SwitchPreference(
            val preference: PreferenceData<Boolean>,
            override val title: String,
            override val subtitle: String? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean, Boolean>() {
            override val icon: ImageVector? = null
            override val isProfileSpecific: Boolean = preference.isProfileSpecificKey()
        }

        /**
         * A [PreferenceItem] that provides a slider to select an integer number.
         */
        data class SliderPreference(
            val value: Int,
            override val title: String,
            override val subtitle: String? = null,
            val valueString: String? = null,
            val preference: PreferenceData<Int>? = null,
            val valueRange: IntProgression = 0..1,
            @IntRange(from = 0) val steps: Int = with(valueRange) { ((last - first) - 1).coerceAtLeast(0) },
            override val enabled: Boolean = true,
            override val isProfileSpecific: Boolean = preference?.isProfileSpecificKey() ?: false,
            override val onValueChanged: suspend (value: Int) -> Unit = {},
        ) : PreferenceItem<Int, Unit>() {
            override val icon: ImageVector? = null
        }

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            val preference: PreferenceData<T>,
            val entries: Map<T, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: T, entries: Map<T, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            val entryEnabledProvider: (value: T) -> Boolean = { true },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: T) -> Boolean = { true },
        ) : PreferenceItem<T, Boolean>() {
            override val isProfileSpecific: Boolean = preference.isProfileSpecificKey()

            internal fun internalSet(value: Any) = preference.set(value as T)
            internal suspend fun internalOnValueChanged(value: Any) = onValueChanged(value as T)
            internal fun internalEntryEnabled(value: Any) = entryEnabledProvider(value as T)

            @Composable
            internal fun internalSubtitleProvider(value: Any?, entries: Map<out Any?, String>) =
                subtitleProvider(value as T, entries as Map<T, String>)
        }

        /**
         * [ListPreference] but with no connection to a [PreferenceData]
         */
        data class BasicListPreference(
            val value: String,
            val entries: Map<String, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: String, entries: Map<String, String>) -> String? =
                { v, e -> subtitle?.format(e[v]) },
            val entryEnabledProvider: (value: String) -> Boolean = { true },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val isProfileSpecific: Boolean = false,
            override val onValueChanged: suspend (value: String) -> Unit = {},
        ) : PreferenceItem<String, Unit>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog.
         * Multiple entries can be selected at the same time.
         */
        @Suppress("UNCHECKED_CAST")
        data class MultiSelectListPreference<T>(
            val preference: PreferenceData<Set<T>>,
            val entries: Map<T, String>,
            override val title: String,
            override val subtitle: String? = "%s",
            val subtitleProvider: @Composable (value: Set<T>, entries: Map<T, String>) -> String? =
                { v, e ->
                    val combined = remember(v, e) {
                        v.mapNotNull { e[it] }
                            .joinToString()
                            .takeUnless { it.isBlank() }
                    }
                        ?: stringResource(MR.strings.none)
                    subtitle?.format(combined)
                },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: Set<T>) -> Boolean = { true },
        ) : PreferenceItem<Set<T>, Boolean>() {
            override val isProfileSpecific: Boolean = preference.isProfileSpecificKey()

            internal fun internalSet(value: Set<Any?>) = preference.set(value as Set<T>)
            internal suspend fun internalOnValueChanged(value: Set<Any?>) = onValueChanged(value as Set<T>)

            @Composable
            internal fun internalSubtitleProvider(value: Set<Any?>, entries: Map<out Any?, String>) =
                subtitleProvider(value as Set<T>, entries as Map<T, String>)
        }

        /**
         * A [PreferenceItem] that shows a EditText in the dialog.
         */
        data class EditTextPreference(
            val preference: PreferenceData<String>,
            override val title: String,
            override val subtitle: String? = "%s",
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (value: String) -> Boolean = { true },
        ) : PreferenceItem<String, Boolean>() {
            override val icon: ImageVector? = null
            override val isProfileSpecific: Boolean = preference.isProfileSpecificKey()
        }

        /**
         * A [PreferenceItem] for individual tracker.
         */
        data class TrackerPreference(
            val tracker: Tracker,
            val login: () -> Unit,
            val logout: () -> Unit,
            override val isProfileSpecific: Boolean = false,
        ) : PreferenceItem<String, Unit>() {
            override val title: String = ""
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: String) -> Unit = {}
        }

        data class InfoPreference(
            override val title: String,
            val showIcon: Boolean = true,
        ) : PreferenceItem<String, Unit>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val isProfileSpecific: Boolean = false
            override val onValueChanged: suspend (value: String) -> Unit = {}
        }

        data class CustomPreference(
            override val title: String,
            override val isProfileSpecific: Boolean = false,
            val content: @Composable () -> Unit,
        ) : PreferenceItem<Unit, Unit>() {
            override val enabled: Boolean = true
            override val subtitle: String? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (value: Unit) -> Unit = {}
        }
    }

    data class PreferenceGroup(
        override val title: String,
        override val enabled: Boolean = true,

        val preferenceItems: List<PreferenceItem<out Any, out Any>>,
    ) : Preference()
}

fun Preference.PreferenceGroup.isFullyProfileSpecific(): Boolean {
    return preferenceItems.isNotEmpty() &&
        preferenceItems.all { it.isProfileSpecific || it is Preference.PreferenceItem.InfoPreference }
}

private fun PreferenceData<*>.isProfileSpecificKey(): Boolean {
    val key = key()
    return ProfileAwarePreferenceStore.Namespace.isNamespacedKey(key) ||
        key.startsWith(CorePreference.appStateKey("profile_")) ||
        key.startsWith(CorePreference.privateKey("profile_"))
}

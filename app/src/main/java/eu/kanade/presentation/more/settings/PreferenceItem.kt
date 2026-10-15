package eu.kanade.presentation.more.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.widget.EditTextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.InfoWidget
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.MultiSelectListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrackingPreferenceWidget
import eu.kanade.presentation.more.settings.widget.preferenceHighlight
import kotlinx.coroutines.launch
import mihon.presentation.kit.control.Stepper
import mihon.presentation.kit.list.SliderRow
import tachiyomi.presentation.core.util.collectAsState

/** Whether the preference being composed is the one settings search opened its screen for. */
val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }

@Composable
fun StatusWrapper(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val visible = item.visible
    val highlighted = item.title == highlightKey
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
    showProfileChip: Boolean,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                val value by item.preference.collectAsState()
                SwitchPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    checked = value,
                    enabled = item.isInteractive,
                    onDisabledClick = item.onDisabledClick,
                    onCheckedChanged = { newValue ->
                        scope.launch {
                            if (item.onValueChanged(newValue)) {
                                item.preference.set(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.SliderPreference -> {
                val value by item.preference.collectAsState()
                SliderRow(
                    title = item.title,
                    value = value,
                    valueRange = item.valueRange,
                    onValueChange = { newValue ->
                        scope.launch {
                            if (item.onValueChanged(newValue)) {
                                item.preference.set(newValue)
                            }
                        }
                    },
                    modifier = Modifier.preferenceHighlight(),
                    subtitle = item.subtitle,
                    valueText = item.valueText(value),
                    steps = item.steps,
                )
            }
            is Preference.PreferenceItem.StepperPreference -> {
                val value by item.preference.collectAsState()
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    widget = {
                        Stepper(
                            value = value,
                            valueRange = item.valueRange,
                            step = item.step,
                            valueFormatter = item.valueFormatter,
                            inputSuffix = item.inputSuffix,
                            decreaseContentDescription = item.decreaseContentDescription,
                            increaseContentDescription = item.increaseContentDescription,
                            editContentDescription = item.editContentDescription,
                            onValueChange = { target ->
                                scope.launch {
                                    if (item.onValueChanged(target)) item.preference.set(target)
                                }
                            },
                        )
                    },
                )
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val value by item.preference.collectAsState()
                ListPreferenceWidget(
                    value = value,
                    title = item.title,
                    subtitle = item.internalSubtitleProvider(value, item.entries),
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    entries = item.entries,
                    entryEnabled = { item.internalEntryEnabled(it!!) },
                    onValueChange = { newValue ->
                        scope.launch {
                            if (item.internalOnValueChanged(newValue!!)) {
                                item.internalSet(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                ListPreferenceWidget(
                    value = item.value,
                    title = item.title,
                    subtitle = item.subtitleProvider(item.value, item.entries),
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    entries = item.entries,
                    entryEnabled = item.entryEnabledProvider,
                    onValueChange = { scope.launch { item.onValueChanged(it) } },
                )
            }
            is Preference.PreferenceItem.MultiSelectListPreference<*> -> {
                val values by item.preference.collectAsState()
                MultiSelectListPreferenceWidget(
                    values = values,
                    title = item.title,
                    subtitle = item.internalSubtitleProvider(values, item.entries),
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    entries = item.entries,
                    onValuesChange = { newValues ->
                        scope.launch {
                            if (item.internalOnValueChanged(newValues)) {
                                item.internalSet(newValues)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    widget = item.widget,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                val values by item.preference.collectAsState()
                EditTextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    isProfileSpecific = showProfileChip && item.isProfileSpecific,
                    value = values,
                    onConfirm = {
                        val accepted = item.onValueChanged(it)
                        if (accepted) item.preference.set(it)
                        accepted
                    },
                )
            }
            is Preference.PreferenceItem.TrackingAccountPreference -> {
                TrackingPreferenceWidget(
                    service = item.account.service,
                    isLoggedIn = item.account.isLoggedIn,
                    displayUsername = item.account.displayUsername,
                    isRefreshing = item.isRefreshing,
                    onRefresh = item.refresh,
                    onClick = { if (item.account.isLoggedIn) item.logout() else item.login() },
                )
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(
                    text = item.title,
                    showIcon = item.showIcon,
                )
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content()
            }
        }
    }
}

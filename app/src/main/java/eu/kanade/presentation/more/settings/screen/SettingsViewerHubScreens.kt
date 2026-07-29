package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.entry.interactions.EntryViewerSettingsFeature
import mihon.entry.interactions.EntryViewerSettingsScreenProjection
import mihon.entry.viewer.settings.ReaderSharedSettingAction
import mihon.entry.viewer.settings.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.ReaderSharedSettingText
import mihon.entry.viewer.settings.ReaderSharedSettingsRegistry
import mihon.entry.viewer.settings.ResolvedReaderSharedToggleSetting
import mihon.entry.viewer.settings.ViewerSettingsCategory
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class AppEntryViewerSettingsScreenProjection :
    EntryViewerSettingsScreenProjection,
    SearchableSettings {

    @Composable
    protected abstract fun getSurfacePreferences(): List<Preference>

    /**
     * Common app-level viewer settings host.
     *
     * Concrete screens contribute only surface-owned preferences. Applicable shared reader declarations are
     * projected here so a new declaration cannot be omitted from an individual capable screen.
     */
    @Composable
    final override fun getPreferences(): List<Preference> {
        val registry = remember { Injekt.get<ReaderSharedSettingsRegistry>() }
        val sharedSettings = remember(registry, surfaceId) { registry.settingsForSurface(surfaceId) }
        val surfacePreferences = getSurfacePreferences()
        if (sharedSettings.isEmpty()) return surfacePreferences

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_general),
                preferenceItems = sharedSettings.map { setting -> setting.toAppPreference() },
            ),
        ) + surfacePreferences
    }
}

object SettingsReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_readers

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.READER)
}

object SettingsPlayerScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_players

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.PLAYER)
}

@Composable
private fun viewerProviderPreferences(category: ViewerSettingsCategory): List<Preference> {
    val navigator = LocalNavigator.currentOrThrow
    val feature = remember { Injekt.get<EntryViewerSettingsFeature>() }

    val destinations = feature.destinations
        .filter { it.category == category }
        .map { destination ->
            val screen = destination.appScreen
            Preference.PreferenceItem.TextPreference(
                title = stringResource(screen.getTitleRes()),
                subtitle = destination.description ?: destination.origin,
                isProfileSpecific = true,
                onClick = { navigator.push(screen) },
            )
        }
    if (category != ViewerSettingsCategory.READER) return destinations

    val sharedSettings = remember {
        Injekt.get<ReaderSharedSettingsRegistry>().rootSettings()
    }
    return sharedSettings.map { setting -> setting.toAppPreference() } + destinations
}

@Composable
private fun ResolvedReaderSharedToggleSetting.toAppPreference(): Preference.PreferenceItem.SwitchPreference {
    val context = androidx.compose.ui.platform.LocalContext.current
    val availability = rememberReaderSharedSettingAvailability(this)
    val presentation = readerSharedSettingAppPresentation(availability, summary)
    return Preference.PreferenceItem.SwitchPreference(
        preference = preference,
        title = title.resolve(context),
        subtitle = presentation.subtitle
            .joinToString(separator = "\n") { text -> text.resolve(context) },
        enabled = presentation.isVisible,
        isInteractive = presentation.isInteractive,
        onDisabledClick = presentation.disabledAction?.let { action ->
            { action.perform(context) }
        },
    )
}

internal data class ReaderSharedSettingAppPresentation(
    val subtitle: List<ReaderSharedSettingText>,
    val isVisible: Boolean,
    val isInteractive: Boolean,
    val disabledAction: ReaderSharedSettingAction?,
)

internal fun readerSharedSettingAppPresentation(
    availability: ReaderSharedSettingAvailability?,
    summary: ReaderSharedSettingText,
): ReaderSharedSettingAppPresentation = when (availability) {
    null -> ReaderSharedSettingAppPresentation(
        subtitle = listOf(summary),
        isVisible = true,
        isInteractive = false,
        disabledAction = null,
    )
    ReaderSharedSettingAvailability.Available -> ReaderSharedSettingAppPresentation(
        subtitle = listOf(summary),
        isVisible = true,
        isInteractive = true,
        disabledAction = null,
    )
    is ReaderSharedSettingAvailability.Disabled -> ReaderSharedSettingAppPresentation(
        subtitle = listOfNotNull(availability.reason, availability.action?.label),
        isVisible = true,
        isInteractive = false,
        disabledAction = availability.action,
    )
}

@Composable
private fun rememberReaderSharedSettingAvailability(
    setting: ResolvedReaderSharedToggleSetting,
): ReaderSharedSettingAvailability? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeGeneration by remember(setting) { mutableIntStateOf(0) }
    var availability by remember(setting) { mutableStateOf<ReaderSharedSettingAvailability?>(null) }

    DisposableEffect(lifecycleOwner, setting) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeGeneration++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(setting, resumeGeneration) {
        availability = setting.resolveAvailability()
    }
    LaunchedEffect(setting) {
        setting.availabilityChanges.collect {
            availability = setting.resolveAvailability()
        }
    }
    return availability
}

internal fun viewerProviderSettingsScreens(
    feature: EntryViewerSettingsFeature,
): List<SearchableSettings> = feature.destinations
    .map { it.appScreen }
    .distinct()

private val mihon.entry.interactions.EntryViewerSettingsDestination.appScreen: AppEntryViewerSettingsScreenProjection
    get() = projection as AppEntryViewerSettingsScreenProjection

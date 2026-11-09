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
import app.katari.core.platform.currentPlatformContext
import app.katari.entry.interactions.media.EntryViewerSettingsDestination
import app.katari.entry.interactions.media.EntryViewerSettingsFeature
import app.katari.entry.interactions.media.EntryViewerSettingsScreenProjection
import app.katari.entry.viewer.settings.ViewerSettingsCategory
import app.katari.entry.viewer.settings.ViewerSettingsProvider
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingAction
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingText
import app.katari.entry.viewer.settings.shared.ResolvedReaderSharedToggleSetting
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

abstract class AppEntryViewerSettingsScreenProjection :
    SearchableSettings(),
    EntryViewerSettingsScreenProjection {

    /**
     * A screen of this surface for one opening. Every settings host gets the instance the resolver registers, so two
     * hosts opening that instance would share one saved state and one set of view models.
     */
    abstract fun newScreen(): AppEntryViewerSettingsScreenProjection

    @Composable
    protected abstract fun getSurfacePreferences(): List<Preference>

    /** The settings of this screen's surface, as the viewer that declares the surface provides them. */
    @Composable
    protected fun surfaceSettings(): ViewerSettingsProvider {
        val feature = LocalAppUiGraph.current.entryViewerSettingsFeature
        return remember(feature) { feature.destinations.first { it.surfaceId == surfaceId }.settings }
    }

    /**
     * Common app-level viewer settings host.
     *
     * Concrete screens contribute only surface-owned preferences. Applicable shared reader declarations are
     * projected here so a new declaration cannot be omitted from an individual capable screen.
     */
    @Composable
    final override fun getPreferences(): List<Preference> {
        val registry = LocalAppUiGraph.current.readerSharedSettingsRegistry
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

class SettingsReaderScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_readers

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.READER)

    override fun isOffered(graph: AppUiGraph): Boolean =
        graph.offersViewerSettings(ViewerSettingsCategory.READER) ||
            graph.readerSharedSettingsRegistry.rootSettings().isNotEmpty()
}

class SettingsPlayerScreen : SearchableSettings() {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_players

    @Composable
    override fun getPreferences(): List<Preference> = viewerProviderPreferences(ViewerSettingsCategory.PLAYER)

    override fun isOffered(graph: AppUiGraph): Boolean = graph.offersViewerSettings(ViewerSettingsCategory.PLAYER)
}

/** Whether a viewer of [category] declares settings, which only the entry types that ship one do. */
private fun AppUiGraph.offersViewerSettings(category: ViewerSettingsCategory): Boolean =
    entryViewerSettingsFeature.destinations.any { it.category == category }

@Composable
private fun viewerProviderPreferences(category: ViewerSettingsCategory): List<Preference> {
    val graph = LocalAppUiGraph.current
    val navigator = LocalNavigator.currentOrThrow
    val feature = graph.entryViewerSettingsFeature

    val destinations = feature.destinations
        .filter { it.category == category }
        .map { destination ->
            val screen = destination.appScreen
            Preference.PreferenceItem.TextPreference(
                title = stringResource(screen.getTitleRes()),
                subtitle = destination.description ?: destination.origin,
                isProfileSpecific = true,
                onClick = { navigator.push(screen.newScreen()) },
            )
        }
    if (category != ViewerSettingsCategory.READER) return destinations

    val sharedSettings = remember(graph) {
        graph.readerSharedSettingsRegistry.rootSettings()
    }
    return sharedSettings.map { setting -> setting.toAppPreference() } + destinations
}

@Composable
private fun ResolvedReaderSharedToggleSetting.toAppPreference(): Preference.PreferenceItem.SwitchPreference {
    val context = currentPlatformContext()
    val availability = rememberReaderSharedSettingAvailability(this)
    val presentation = readerSharedSettingAppPresentation(availability, summary)
    return Preference.PreferenceItem.SwitchPreference(
        preference = preference,
        title = title.resolve(context),
        subtitle = presentation.subtitle
            .joinToString(separator = "\n") { text -> text.resolve(context) },
        visible = presentation.isVisible,
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

fun viewerProviderSettingsScreens(
    feature: EntryViewerSettingsFeature,
): List<SearchableSettings> = feature.destinations
    .map { it.appScreen }
    .distinct()
    .map { it.newScreen() }

private val EntryViewerSettingsDestination.appScreen: AppEntryViewerSettingsScreenProjection
    get() = projection as AppEntryViewerSettingsScreenProjection

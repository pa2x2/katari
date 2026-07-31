package mihon.entry.interactions.book.reader.translation

import android.content.Intent
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAction
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ReaderSharedSettingId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingText
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedTogglePreferenceBinding
import mihon.entry.viewer.settings.shared.ReaderSharedToggleSetting
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.host.TranslationSettingsNavigation

internal class BookAutomaticTranslationSettingsProvider(
    processorRegistry: BookReaderProcessorRegistry,
    private val preferences: BookAutomaticTranslationPreferences,
    private val translationHostActions: TranslationHostActions,
) : ReaderSharedSettingsProvider {
    override val potentialCapabilities = processorRegistry.potentialReaderCapabilities()
    override val potentialCapabilitiesBySettingsSurface =
        processorRegistry.potentialReaderCapabilitiesBySettingsSurface()
    private val automaticSelectionPreferences = potentialCapabilitiesBySettingsSurface.keys.associateWith(
        preferences::automaticSelectionEnabled,
    )

    val automaticSelection = ReaderSharedToggleSetting(
        id = AUTOMATIC_SELECTION_SETTING_ID,
        title = ReaderSharedSettingText { context ->
            context.getString(R.string.book_reader_automatic_translation)
        },
        summary = ReaderSharedSettingText { context ->
            context.getString(R.string.book_reader_automatic_translation_summary)
        },
        preferenceBinding = ReaderSharedTogglePreferenceBinding.PerSettingsSurface(
            automaticSelectionPreferences,
        ),
        defaultValue = false,
        requiredCapabilities = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        ),
        availabilityChanges = translationHostActions.selectedEngine.changes().map { Unit },
        resolveAvailability = {
            translationHostActions.deviceAvailability().toReaderAvailability()
        },
    )

    override val settings = listOf(automaticSelection)

    fun automaticSelectionEnabled(settingsSurfaceId: String) =
        requireNotNull(automaticSelectionPreferences[settingsSurfaceId]) {
            "No automatic translation preference is registered for viewer settings surface '$settingsSurfaceId'"
        }

    private fun TranslationDeviceAvailability.toReaderAvailability(): ReaderSharedSettingAvailability {
        return when (this) {
            TranslationDeviceAvailability.Available -> ReaderSharedSettingAvailability.Available
            TranslationDeviceAvailability.EngineNotConfigured ->
                disabled(R.string.book_reader_translation_engine_not_configured)
            is TranslationDeviceAvailability.SelectedEngineMissing ->
                disabled(R.string.book_reader_translation_engine_missing, engine.value)
            is TranslationDeviceAvailability.SelectedEngineUnavailable ->
                if (reason.isNullOrBlank()) {
                    disabled(R.string.book_reader_translation_engine_unavailable, engine.value)
                } else {
                    disabled(
                        R.string.book_reader_translation_engine_unavailable_reason,
                        engine.value,
                        checkNotNull(reason),
                    )
                }
            is TranslationDeviceAvailability.UnsupportedOs ->
                disabled(R.string.book_reader_translation_unsupported_os, minimumApi)
            TranslationDeviceAvailability.TranslationServiceMissing ->
                disabled(R.string.book_reader_translation_service_missing)
            is TranslationDeviceAvailability.ProviderFailure ->
                disabled(R.string.book_reader_translation_provider_failure, reason)
        }
    }

    private fun disabled(resource: Int, vararg arguments: Any): ReaderSharedSettingAvailability.Disabled {
        return ReaderSharedSettingAvailability.Disabled(
            reason = ReaderSharedSettingText { context -> context.getString(resource, *arguments) },
            action = ReaderSharedSettingAction(
                label = ReaderSharedSettingText { context ->
                    context.getString(R.string.book_reader_configure_translation)
                },
                perform = { context ->
                    context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply {
                            action = TranslationSettingsNavigation.ACTION_OPEN_SETTINGS
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        ?.let(context::startActivity)
                },
            ),
        )
    }

    companion object {
        val AUTOMATIC_SELECTION_SETTING_ID = ReaderSharedSettingId("translation.automatic-selection")
    }
}

package mihon.entry.interactions.book.reader.translation

import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAction
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ReaderSharedSettingText
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedTogglePreferenceBinding
import mihon.entry.viewer.settings.shared.ReaderSharedToggleSetting
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.translation.api.availability.TranslationDeviceAvailability
import mihon.translation.api.host.TranslationHostActions
import mihon.translation.api.host.TranslationSettingsDestination
import tachiyomi.i18n.MR
import tachiyomi.i18n.book_reader_automatic_translation
import tachiyomi.i18n.book_reader_automatic_translation_summary
import tachiyomi.i18n.book_reader_configure_translation
import tachiyomi.i18n.book_reader_translation_engine_missing
import tachiyomi.i18n.book_reader_translation_engine_not_configured
import tachiyomi.i18n.book_reader_translation_engine_unavailable
import tachiyomi.i18n.book_reader_translation_engine_unavailable_reason
import tachiyomi.i18n.book_reader_translation_provider_failure
import tachiyomi.i18n.book_reader_translation_service_missing
import tachiyomi.i18n.book_reader_translation_unsupported_os
import tachiyomi.i18n.platform.stringResource

internal class BookAutomaticTranslationSettingsProvider(
    processorRegistry: BookReaderProcessorRegistry,
    private val preferences: BookAutomaticTranslationPreferences,
    private val translationHostActions: TranslationHostActions,
    private val translationSettings: TranslationSettingsDestination,
) : ReaderSharedSettingsProvider {
    override val potentialCapabilities = processorRegistry.potentialReaderCapabilities()
    override val potentialCapabilitiesBySettingsSurface =
        processorRegistry.potentialReaderCapabilitiesBySettingsSurface()
    private val automaticSelectionPreferences = potentialCapabilitiesBySettingsSurface.keys.associateWith(
        preferences::automaticSelectionEnabled,
    )

    val automaticSelection = ReaderSharedToggleSetting(
        id = BookAutomaticTranslationPreferences.AUTOMATIC_SELECTION_SETTING_ID,
        title = ReaderSharedSettingText { context ->
            context.stringResource(MR.strings.book_reader_automatic_translation)
        },
        summary = ReaderSharedSettingText { context ->
            context.stringResource(MR.strings.book_reader_automatic_translation_summary)
        },
        preferenceBinding = ReaderSharedTogglePreferenceBinding.PerSettingsSurface(
            automaticSelectionPreferences,
        ),
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
                disabled(MR.strings.book_reader_translation_engine_not_configured)
            is TranslationDeviceAvailability.SelectedEngineMissing ->
                disabled(MR.strings.book_reader_translation_engine_missing, engine.value)
            is TranslationDeviceAvailability.SelectedEngineUnavailable ->
                if (reason.isNullOrBlank()) {
                    disabled(MR.strings.book_reader_translation_engine_unavailable, engine.value)
                } else {
                    disabled(
                        MR.strings.book_reader_translation_engine_unavailable_reason,
                        engine.value,
                        checkNotNull(reason),
                    )
                }
            is TranslationDeviceAvailability.UnsupportedOs ->
                disabled(MR.strings.book_reader_translation_unsupported_os, minimumApi)
            TranslationDeviceAvailability.TranslationServiceMissing ->
                disabled(MR.strings.book_reader_translation_service_missing)
            is TranslationDeviceAvailability.ProviderFailure ->
                disabled(MR.strings.book_reader_translation_provider_failure, reason)
        }
    }

    private fun disabled(resource: StringResource, vararg arguments: Any): ReaderSharedSettingAvailability.Disabled {
        return ReaderSharedSettingAvailability.Disabled(
            reason = ReaderSharedSettingText { context -> context.stringResource(resource, *arguments) },
            action = ReaderSharedSettingAction(
                label = ReaderSharedSettingText { context ->
                    context.stringResource(MR.strings.book_reader_configure_translation)
                },
                perform = translationSettings::openTranslationSettings,
            ),
        )
    }
}

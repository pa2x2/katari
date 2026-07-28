package mihon.entry.interactions.book

import kotlinx.coroutines.flow.map
import mihon.entry.viewer.settings.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.ReaderSharedSettingId
import mihon.entry.viewer.settings.ReaderSharedSettingText
import mihon.entry.viewer.settings.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.ReaderSharedToggleSetting
import mihon.entry.viewer.settings.StandardReaderCapabilities
import mihon.translation.api.TranslationDeviceAvailability
import mihon.translation.api.TranslationHostActions

internal class BookAutomaticTranslationSettingsProvider(
    processorRegistry: BookProcessorRegistry,
    private val translationHostActions: TranslationHostActions,
) : ReaderSharedSettingsProvider {
    override val potentialCapabilities = processorRegistry.potentialReaderCapabilities()

    val automaticSelection = ReaderSharedToggleSetting(
        id = AUTOMATIC_SELECTION_SETTING_ID,
        title = ReaderSharedSettingText { context ->
            context.getString(R.string.book_reader_automatic_translation)
        },
        summary = ReaderSharedSettingText { context ->
            context.getString(R.string.book_reader_automatic_translation_summary)
        },
        preference = translationHostActions.automaticSelectionEnabled,
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

    private fun TranslationDeviceAvailability.toReaderAvailability(): ReaderSharedSettingAvailability {
        return when (this) {
            TranslationDeviceAvailability.Available -> ReaderSharedSettingAvailability.Available
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
            ReaderSharedSettingText { context -> context.getString(resource, *arguments) },
        )
    }

    companion object {
        val AUTOMATIC_SELECTION_SETTING_ID = ReaderSharedSettingId("translation.automatic-selection")
    }
}

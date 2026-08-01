package mihon.entry.interactions.reader.preparation

import mihon.entry.viewer.settings.shared.ReaderCapabilityId
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ReaderSharedSettingText
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.shared.ReaderSharedTogglePreferenceBinding
import mihon.entry.viewer.settings.shared.ReaderSharedToggleSetting
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

class ReaderChapterPreparationSettingsProvider(
    preferences: ReaderChapterPreparationPreferences,
    override val potentialCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>>,
) : ReaderSharedSettingsProvider {
    override val potentialCapabilities: Set<ReaderCapabilityId> =
        potentialCapabilitiesBySettingsSurface.values.flatten().toSet()
    private val preferencesBySurface = potentialCapabilitiesBySettingsSurface
        .filterValues { capabilities -> StandardReaderCapabilities.NextChapterPreparation in capabilities }
        .keys
        .associateWith(preferences::prepareNextChapter)

    init {
        preferences.completeLegacyMigration(preferencesBySurface.keys)
    }

    private val prepareNextChapter = preferencesBySurface.takeIf { it.isNotEmpty() }?.let { surfacePreferences ->
        ReaderSharedToggleSetting(
            id = PREPARE_NEXT_CHAPTER_SETTING_ID,
            title = ReaderSharedSettingText { context ->
                context.stringResource(MR.strings.pref_prepare_next_chapter)
            },
            summary = ReaderSharedSettingText { context ->
                context.stringResource(MR.strings.pref_prepare_next_chapter_summary)
            },
            preferenceBinding = ReaderSharedTogglePreferenceBinding.PerSettingsSurface(surfacePreferences),
            defaultValue = false,
            requiredCapabilities = setOf(StandardReaderCapabilities.NextChapterPreparation),
            resolveAvailability = { ReaderSharedSettingAvailability.Available },
        )
    }

    override val settings = listOfNotNull(prepareNextChapter)

    companion object {
        val PREPARE_NEXT_CHAPTER_SETTING_ID = StandardReaderSharedSettingIds.NextChapterPreparation
    }
}

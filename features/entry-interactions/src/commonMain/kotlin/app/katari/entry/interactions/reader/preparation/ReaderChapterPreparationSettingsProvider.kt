package app.katari.entry.interactions.reader.preparation

import app.katari.entry.viewer.settings.shared.ReaderCapabilityId
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingText
import app.katari.entry.viewer.settings.shared.ReaderSharedSettingsProvider
import app.katari.entry.viewer.settings.shared.ReaderSharedTogglePreferenceBinding
import app.katari.entry.viewer.settings.shared.ReaderSharedToggleSetting
import app.katari.entry.viewer.settings.shared.StandardReaderCapabilities
import app.katari.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

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
            requiredCapabilities = setOf(StandardReaderCapabilities.NextChapterPreparation),
            resolveAvailability = { ReaderSharedSettingAvailability.Available },
        )
    }

    override val settings = listOfNotNull(prepareNextChapter)

    companion object {
        val PREPARE_NEXT_CHAPTER_SETTING_ID = StandardReaderSharedSettingIds.NextChapterPreparation
    }
}

package mihon.entry.interactions.reader.preparation

import mihon.entry.viewer.settings.ReaderCapabilityId
import mihon.entry.viewer.settings.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.ReaderSharedSettingId
import mihon.entry.viewer.settings.ReaderSharedSettingText
import mihon.entry.viewer.settings.ReaderSharedSettingsProvider
import mihon.entry.viewer.settings.ReaderSharedTogglePreferenceBinding
import mihon.entry.viewer.settings.ReaderSharedToggleSetting
import mihon.entry.viewer.settings.StandardReaderCapabilities
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

class ReaderChapterPreparationSettingsProvider(
    preferences: ReaderChapterPreparationPreferences,
    override val potentialCapabilitiesBySettingsSurface: Map<String, Set<ReaderCapabilityId>>,
) : ReaderSharedSettingsProvider {
    override val potentialCapabilities: Set<ReaderCapabilityId> =
        potentialCapabilitiesBySettingsSurface.values.flatten().toSet()

    val prepareNextChapter = ReaderSharedToggleSetting(
        id = PREPARE_NEXT_CHAPTER_SETTING_ID,
        title = ReaderSharedSettingText { context ->
            context.stringResource(MR.strings.pref_prepare_next_chapter)
        },
        summary = ReaderSharedSettingText { context ->
            context.stringResource(MR.strings.pref_prepare_next_chapter_summary)
        },
        preferenceBinding = ReaderSharedTogglePreferenceBinding.Global(preferences.prepareNextChapter),
        defaultValue = false,
        requiredCapabilities = setOf(StandardReaderCapabilities.NextChapterPreparation),
        resolveAvailability = { ReaderSharedSettingAvailability.Available },
    )

    override val settings = listOf(prepareNextChapter)

    companion object {
        val PREPARE_NEXT_CHAPTER_SETTING_ID = ReaderSharedSettingId("reader.prepare-next-chapter")
    }
}

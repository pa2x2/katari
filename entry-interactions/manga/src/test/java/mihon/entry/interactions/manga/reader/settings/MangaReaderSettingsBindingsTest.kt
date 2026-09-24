package mihon.entry.interactions.manga.reader.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingEntryBinder
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class MangaReaderSettingsBindingsTest {

    @Test
    fun `bindings cover every declared setting and shared definition`() = runTest {
        val provider = provider()
        val binder = RecordingViewerSettingBinder()

        val bindings = MangaReaderSettingsBindings.create(provider, binder, entryId = 42L)

        assertEquals(provider.settings.map { it.id }.toSet(), binder.boundIds)
        assertEquals(provider.sharedSettingDefinitions.keys, bindings.sharedSettings.keys)
    }

    @Test
    fun `typed accessors resolve their declared definitions`() = runTest {
        val provider = provider()
        val bindings = MangaReaderSettingsBindings.create(provider, RecordingViewerSettingBinder(), entryId = 42L)

        assertEquals(provider.reading.readingMode.id, bindings.readingMode.definition.id)
        assertEquals(provider.reading.prepareNextChapter.id, bindings.prepareNextChapter.definition.id)
        assertEquals(provider.webtoon.sidePadding.id, bindings.webtoonSidePadding.definition.id)
        assertEquals(provider.colorFilter.grayscale.id, bindings.grayscale.definition.id)
    }

    @Test
    fun `reset clears only entry override capable settings`() = runTest {
        val provider = provider()
        val binder = RecordingViewerSettingBinder()

        val bindings = MangaReaderSettingsBindings.create(provider, binder, entryId = 42L)
        bindings.clearEntryOverrides()

        val overridableKeys = provider.settings
            .filter { it.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE }
            .map { it.id.key }
            .toSet()
        assertEquals(overridableKeys, binder.clearedIds.map { it.key }.toSet())
        assertFalse("skip_read" in binder.clearedIds.map { it.key })
    }

    private fun provider() = MangaReaderSettingsProvider(
        preferenceStore = InMemoryPreferenceStore(),
        chapterPreparationPreferences = ReaderChapterPreparationPreferences(InMemoryPreferenceStore()),
    )
}

private class RecordingViewerSettingBinder : ViewerSettingBinder {
    val boundIds = linkedSetOf<ViewerSettingId>()
    val clearedIds = linkedSetOf<ViewerSettingId>()

    override fun <T> bind(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ViewerSettingBinding<T> = throw UnsupportedOperationException("Bindings are created through initializeEntry")

    override suspend fun initializeEntry(entryId: Long): ViewerSettingEntryBinder =
        RecordingEntryBinder(entryId, this)

    override suspend fun <T> resolve(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ResolvedViewerSetting<T> = throw UnsupportedOperationException("Resolution is unnecessary for this fake")
}

private class RecordingEntryBinder(
    override val entryId: Long,
    private val owner: RecordingViewerSettingBinder,
) : ViewerSettingEntryBinder {
    override fun <T> bind(definition: ViewerSettingDefinition<T>): ViewerSettingBinding<T> {
        owner.boundIds += definition.id
        return RecordingBinding(definition, entryId, owner)
    }
}

private class RecordingBinding<T>(
    override val definition: ViewerSettingDefinition<T>,
    override val entryId: Long?,
    private val owner: RecordingViewerSettingBinder,
) : ViewerSettingBinding<T> {
    override val state: StateFlow<ResolvedViewerSetting<T>> = MutableStateFlow(
        ResolvedViewerSetting(
            effectiveValue = definition.processorDefault,
            source = ViewerSettingSource.PROCESSOR_DEFAULT,
            processorDefault = definition.processorDefault,
            profileValue = null,
            entryOverride = null,
        ),
    )

    override fun resolveProfile(): ResolvedViewerSetting<T> = state.value

    override fun setProfileValue(value: T) = Unit

    override fun resetProfileValue() = Unit

    override suspend fun setEntryOverride(value: T) = Unit

    override suspend fun clearEntryOverride() {
        owner.clearedIds += definition.id
    }
}

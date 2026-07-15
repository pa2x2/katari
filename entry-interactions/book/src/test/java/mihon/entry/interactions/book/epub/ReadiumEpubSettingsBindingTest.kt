@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingSource
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReadiumEpubSettingsBindingTest {

    @Test
    fun `provider defaults map to explicit Readium preferences`() {
        val preferences = toReadiumPreferences(
            appearance = AppearanceValues(
                theme = ReadiumEpubSettingsProvider.THEME_LIGHT,
                fontFamily = ReadiumEpubSettingsProvider.FONT_PUBLISHER,
                fontSizePercent = 100,
                pageMarginsPercent = 100,
                textNormalization = false,
            ),
            textLayout = TextLayoutValues(
                lineHeightPercent = 120,
                publisherStyles = true,
                textAlignment = ReadiumEpubSettingsProvider.ALIGN_PUBLISHER,
            ),
            pageLayout = PageLayoutValues(
                layoutMode = ReadiumEpubSettingsProvider.LAYOUT_PAGINATED,
                columnCount = ReadiumEpubSettingsProvider.COLUMNS_AUTO,
            ),
        )

        assertEquals(
            EpubPreferences(
                theme = Theme.LIGHT,
                fontSize = 1.0,
                lineHeight = 1.2,
                pageMargins = 1.0,
                publisherStyles = true,
                scroll = false,
                columnCount = ColumnCount.AUTO,
                textNormalization = false,
            ),
            preferences,
        )
    }

    @Test
    fun `custom portable values map to Readium types and scales`() {
        val preferences = toReadiumPreferences(
            appearance = AppearanceValues(
                theme = ReadiumEpubSettingsProvider.THEME_DARK,
                fontFamily = ReadiumEpubSettingsProvider.FONT_OPEN_DYSLEXIC,
                fontSizePercent = 175,
                pageMarginsPercent = 250,
                textNormalization = true,
            ),
            textLayout = TextLayoutValues(
                lineHeightPercent = 150,
                publisherStyles = false,
                textAlignment = ReadiumEpubSettingsProvider.ALIGN_JUSTIFY,
            ),
            pageLayout = PageLayoutValues(
                layoutMode = ReadiumEpubSettingsProvider.LAYOUT_SCROLLING,
                columnCount = ReadiumEpubSettingsProvider.COLUMNS_TWO,
            ),
        )

        assertEquals(Theme.DARK, preferences.theme)
        assertEquals(FontFamily.OPEN_DYSLEXIC, preferences.fontFamily)
        assertEquals(1.75, preferences.fontSize)
        assertEquals(1.5, preferences.lineHeight)
        assertEquals(2.5, preferences.pageMargins)
        assertEquals(false, preferences.publisherStyles)
        assertEquals(TextAlign.JUSTIFY, preferences.textAlign)
        assertEquals(true, preferences.scroll)
        assertEquals(ColumnCount.TWO, preferences.columnCount)
        assertEquals(true, preferences.textNormalization)
    }

    @Test
    fun `runtime preferences react to changes from the shared binding`() = runTest {
        val provider = ReadiumEpubSettingsProvider(InMemoryPreferenceStore())
        val binder = TestViewerSettingBinder()
        val settings = ReadiumEpubSettingsBinding(provider, binder, entryId = 7L)
        val nextPreferences = async(start = CoroutineStart.UNDISPATCHED) {
            settings.changes.first()
        }
        runCurrent()

        binder.setValue(provider.themeSetting, ReadiumEpubSettingsProvider.THEME_SEPIA)

        assertEquals(Theme.SEPIA, nextPreferences.await().theme)
    }
}

private class TestViewerSettingBinder : ViewerSettingBinder {
    private val bindings = mutableMapOf<Pair<ViewerSettingId, Long?>, TestViewerSettingBinding<*>>()

    override fun <T> bind(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ViewerSettingBinding<T> = binding(definition, entryId)

    override suspend fun <T> resolve(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ResolvedViewerSetting<T> = binding(definition, entryId).state.value

    fun <T> setValue(definition: ViewerSettingDefinition<T>, value: T, entryId: Long? = null) {
        binding(definition, entryId).setValue(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> binding(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): TestViewerSettingBinding<T> = bindings.getOrPut(definition.id to entryId) {
        TestViewerSettingBinding(definition, entryId)
    } as TestViewerSettingBinding<T>
}

private class TestViewerSettingBinding<T>(
    override val definition: ViewerSettingDefinition<T>,
    override val entryId: Long?,
) : ViewerSettingBinding<T> {
    private val mutableState = MutableStateFlow(resolved(definition.processorDefault))
    override val state: StateFlow<ResolvedViewerSetting<T>> = mutableState

    fun setValue(value: T) {
        mutableState.value = resolved(value)
    }

    override fun resolveProfile(): ResolvedViewerSetting<T> = state.value

    override fun setProfileValue(value: T) = setValue(value)

    override fun resetProfileValue() = setValue(definition.processorDefault)

    override suspend fun setEntryOverride(value: T) = setValue(value)

    override suspend fun clearEntryOverride() = setValue(definition.processorDefault)

    private fun resolved(value: T) = ResolvedViewerSetting(
        effectiveValue = value,
        source = if (entryId == null) ViewerSettingSource.PROFILE else ViewerSettingSource.ENTRY,
        processorDefault = definition.processorDefault,
        profileValue = if (entryId == null) value else definition.processorDefault,
        entryOverride = if (entryId == null) null else value,
    )
}

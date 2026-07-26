@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package mihon.entry.interactions.book.prose

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingSource
import org.junit.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.assertFalse

class HtmlProseSettingsBindingTest {
    @Test
    fun `reader initialization waits for the entry layout override`() = runTest {
        val provider = HtmlProseSettingsProvider(InMemoryPreferenceStore())
        val binder = DeferredLayoutBinder(
            layoutId = provider.layoutModeSetting.id,
            resolvedLayout = HtmlProseSettingsProvider.LAYOUT_SCROLLING,
        )
        val settings = HtmlProseSettingsBinding(provider, binder, entryId = 7L)

        val awaiting = async(start = CoroutineStart.UNDISPATCHED) {
            settings.awaitInitialLayoutMode()
        }
        runCurrent()

        assertFalse(awaiting.isCompleted)

        binder.publishLayoutOverride(provider.layoutModeSetting, entryId = 7L)
        awaiting.await()
    }
}

private class DeferredLayoutBinder(
    private val layoutId: ViewerSettingId,
    private val resolvedLayout: String,
) : ViewerSettingBinder {
    private val bindings = mutableMapOf<Pair<ViewerSettingId, Long?>, DeferredViewerSettingBinding<*>>()

    override fun <T> bind(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ViewerSettingBinding<T> = binding(definition, entryId)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> resolve(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ResolvedViewerSetting<T> {
        if (definition.id != layoutId) return binding(definition, entryId).state.value
        return resolved(
            definition = definition as ViewerSettingDefinition<String>,
            value = resolvedLayout,
            entryId = entryId,
        ) as ResolvedViewerSetting<T>
    }

    fun publishLayoutOverride(
        definition: ViewerSettingDefinition<String>,
        entryId: Long,
    ) {
        binding(definition, entryId).setValue(resolvedLayout)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> binding(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): DeferredViewerSettingBinding<T> = bindings.getOrPut(definition.id to entryId) {
        DeferredViewerSettingBinding(definition, entryId)
    } as DeferredViewerSettingBinding<T>
}

private class DeferredViewerSettingBinding<T>(
    override val definition: ViewerSettingDefinition<T>,
    override val entryId: Long?,
) : ViewerSettingBinding<T> {
    private val mutableState = MutableStateFlow(resolved(definition, definition.processorDefault, entryId))
    override val state: StateFlow<ResolvedViewerSetting<T>> = mutableState

    fun setValue(value: T) {
        mutableState.value = resolved(definition, value, entryId)
    }

    override fun resolveProfile(): ResolvedViewerSetting<T> = state.value

    override fun setProfileValue(value: T) = setValue(value)

    override fun resetProfileValue() = setValue(definition.processorDefault)

    override suspend fun setEntryOverride(value: T) = setValue(value)

    override suspend fun clearEntryOverride() = setValue(definition.processorDefault)
}

private fun <T> resolved(
    definition: ViewerSettingDefinition<T>,
    value: T,
    entryId: Long?,
) = ResolvedViewerSetting(
    effectiveValue = value,
    source = if (entryId == null) ViewerSettingSource.PROFILE else ViewerSettingSource.ENTRY,
    processorDefault = definition.processorDefault,
    profileValue = if (entryId == null) value else null,
    entryOverride = if (entryId == null) null else value,
)

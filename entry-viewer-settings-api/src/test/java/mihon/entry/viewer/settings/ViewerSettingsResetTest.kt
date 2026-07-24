package mihon.entry.viewer.settings

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ViewerSettingsResetTest {

    @Test
    fun `reset removes every profile value and current entry override`() = runTest {
        val store = InMemoryPreferenceStore()
        val profileOnlyPreference = store.getBoolean("enabled", false).apply { set(true) }
        val overridablePreference = store.getInt("mode", 1).apply { set(2) }
        val profileOnly = ViewerSettingDefinition(
            id = ViewerSettingId("test.reader", "enabled"),
            scope = ViewerSettingScope.PROFILE_ONLY,
            processorDefault = false,
            profilePreference = profileOnlyPreference,
            codec = ViewerSettingCodecs.Boolean,
        )
        val overridable = ViewerSettingDefinition(
            id = ViewerSettingId("test.reader", "mode"),
            scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
            processorDefault = 1,
            profilePreference = overridablePreference,
            codec = ViewerSettingCodecs.Int,
        )
        val provider = object : ViewerSettingsProvider {
            override val id = "test.reader"
            override val category = ViewerSettingsCategory.READER
            override val displayName = "Test reader"
            override val settings = listOf(profileOnly, overridable)
        }
        val binder = RecordingViewerSettingBinder()

        binder.resetSettings(provider, entryId = 42L)

        profileOnlyPreference.get() shouldBe false
        profileOnlyPreference.isSet() shouldBe false
        overridablePreference.get() shouldBe 1
        overridablePreference.isSet() shouldBe false
        binder.clearedOverrides shouldContainExactly listOf(42L to overridable.id)
    }

    @Test
    fun `profile reset does not attempt to clear entry overrides without an entry`() = runTest {
        val preference = InMemoryPreferenceStore().getInt("mode", 1).apply { set(2) }
        val definition = ViewerSettingDefinition(
            id = ViewerSettingId("test.reader", "mode"),
            scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
            processorDefault = 1,
            profilePreference = preference,
            codec = ViewerSettingCodecs.Int,
        )
        val provider = object : ViewerSettingsProvider {
            override val id = "test.reader"
            override val category = ViewerSettingsCategory.READER
            override val displayName = "Test reader"
            override val settings = listOf(definition)
        }
        val binder = RecordingViewerSettingBinder()

        binder.resetSettings(provider)

        preference.isSet() shouldBe false
        binder.clearedOverrides shouldBe emptyList()
    }
}

private class RecordingViewerSettingBinder : ViewerSettingBinder {
    val clearedOverrides = mutableListOf<Pair<Long, ViewerSettingId>>()

    override fun <T> bind(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ViewerSettingBinding<T> {
        return object : ViewerSettingBinding<T> {
            override val definition = definition
            override val entryId = entryId
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
                clearedOverrides += checkNotNull(entryId) to definition.id
            }
        }
    }

    override suspend fun <T> resolve(
        definition: ViewerSettingDefinition<T>,
        entryId: Long?,
    ): ResolvedViewerSetting<T> = bind(definition, entryId).state.value
}

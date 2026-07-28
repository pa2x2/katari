package mihon.entry.viewer.settings

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderSharedSettingsRegistryTest {
    private val preference = InMemoryPreferenceStore().getBoolean("automatic", false)
    private val setting = ReaderSharedToggleSetting(
        id = ReaderSharedSettingId("translation.automatic-selection"),
        title = ReaderSharedSettingText { "Translate selected text automatically" },
        summary = ReaderSharedSettingText { "Summary" },
        preference = preference,
        defaultValue = false,
        requiredCapabilities = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        ),
        resolveAvailability = { ReaderSharedSettingAvailability.Available },
    )

    @Test
    fun `one declaration projects globally and into capable sessions`() {
        val registry = registry(
            setOf(
                StandardReaderCapabilities.StableTextSelection,
                StandardReaderCapabilities.SelectionAnchoring,
            ),
        )

        registry.globalSettings() shouldContainExactly listOf(setting)
        registry.settingsFor(
            setOf(
                StandardReaderCapabilities.StableTextSelection,
                StandardReaderCapabilities.SelectionAnchoring,
            ),
        ) shouldContainExactly listOf(setting)
        registry.settingsFor(setOf(StandardReaderCapabilities.StableTextSelection)) shouldBe emptyList()
    }

    @Test
    fun `reset restores the declared default`() {
        preference.set(true)

        setting.reset()

        preference.get() shouldBe false
    }

    @Test
    fun `availability does not overwrite a stored enabled value`() = kotlinx.coroutines.test.runTest {
        preference.set(true)
        val unavailableSetting = ReaderSharedToggleSetting(
            id = ReaderSharedSettingId("translation.unavailable-test"),
            title = ReaderSharedSettingText { "Title" },
            summary = ReaderSharedSettingText { "Summary" },
            preference = preference,
            defaultValue = false,
            requiredCapabilities = setOf(StandardReaderCapabilities.StableTextSelection),
            resolveAvailability = {
                ReaderSharedSettingAvailability.Disabled(ReaderSharedSettingText { "Unavailable" })
            },
        )

        unavailableSetting.resolveAvailability()

        preference.get() shouldBe true
    }

    private fun registry(capabilities: Set<ReaderCapabilityId>) = ReaderSharedSettingsRegistry(
        listOf(
            object : ReaderSharedSettingsProvider {
                override val potentialCapabilities = capabilities
                override val settings = listOf(setting)
            },
        ),
    )
}

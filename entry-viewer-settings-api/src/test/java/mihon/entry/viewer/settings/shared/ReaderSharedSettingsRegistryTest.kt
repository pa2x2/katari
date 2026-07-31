package mihon.entry.viewer.settings

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderSharedSettingsRegistryTest {
    private val capableSurfaceId = "builtin.book.prose.html"
    private val secondCapableSurfaceId = "builtin.book.alternate"
    private val unsupportedSurfaceId = "builtin.book.fixed-layout"
    private val preference = InMemoryPreferenceStore().getBoolean("automatic", false)
    private val secondPreference = InMemoryPreferenceStore().getBoolean("automatic-alternate", false)
    private val setting = ReaderSharedToggleSetting(
        id = ReaderSharedSettingId("translation.automatic-selection"),
        title = ReaderSharedSettingText { "Translate selected text automatically" },
        summary = ReaderSharedSettingText { "Summary" },
        preferenceBinding = ReaderSharedTogglePreferenceBinding.PerSettingsSurface(
            mapOf(
                capableSurfaceId to preference,
                secondCapableSurfaceId to secondPreference,
            ),
        ),
        defaultValue = false,
        requiredCapabilities = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        ),
        resolveAvailability = { ReaderSharedSettingAvailability.Available },
    )

    @Test
    fun `per-surface declarations project only into capable surfaces and sessions`() {
        val registry = registry(
            setOf(
                StandardReaderCapabilities.StableTextSelection,
                StandardReaderCapabilities.SelectionAnchoring,
            ),
        )

        registry.rootSettings() shouldBe emptyList()
        registry.settingsForSurface(capableSurfaceId).map { it.declaration } shouldContainExactly listOf(setting)
        registry.settingsForSurface(unsupportedSurfaceId) shouldBe emptyList()
        registry.settingsFor(
            setOf(
                StandardReaderCapabilities.StableTextSelection,
                StandardReaderCapabilities.SelectionAnchoring,
            ),
            capableSurfaceId,
        ).map { it.declaration } shouldContainExactly listOf(setting)
        registry.settingsFor(
            setOf(StandardReaderCapabilities.StableTextSelection),
            capableSurfaceId,
        ) shouldBe emptyList()
    }

    @Test
    fun `reset restores the declared default`() {
        preference.set(true)

        registry(
            setOf(
                StandardReaderCapabilities.StableTextSelection,
                StandardReaderCapabilities.SelectionAnchoring,
            ),
        ).settingsForSurface(capableSurfaceId).single().reset()

        preference.get() shouldBe false
    }

    @Test
    fun `global bindings remain one preference across capable surfaces`() {
        val globalSetting = ReaderSharedToggleSetting(
            id = ReaderSharedSettingId("reader.global-test"),
            title = ReaderSharedSettingText { "Global" },
            summary = ReaderSharedSettingText { "Summary" },
            preferenceBinding = ReaderSharedTogglePreferenceBinding.Global(preference),
            defaultValue = false,
            requiredCapabilities = setOf(StandardReaderCapabilities.StableTextSelection),
            resolveAvailability = { ReaderSharedSettingAvailability.Available },
        )
        val registry = registry(
            capabilities = setOf(StandardReaderCapabilities.StableTextSelection),
            declaredSettings = listOf(globalSetting),
        )

        registry.rootSettings().single().settingsSurfaceId shouldBe null
        registry.settingsForSurface(capableSurfaceId).single().preference shouldBe preference
        registry.settingsForSurface(secondCapableSurfaceId).single().preference shouldBe preference
    }

    @Test
    fun `availability does not overwrite a stored enabled value`() = kotlinx.coroutines.test.runTest {
        preference.set(true)
        val unavailableSetting = ReaderSharedToggleSetting(
            id = ReaderSharedSettingId("translation.unavailable-test"),
            title = ReaderSharedSettingText { "Title" },
            summary = ReaderSharedSettingText { "Summary" },
            preferenceBinding = ReaderSharedTogglePreferenceBinding.Global(preference),
            defaultValue = false,
            requiredCapabilities = setOf(StandardReaderCapabilities.StableTextSelection),
            resolveAvailability = {
                ReaderSharedSettingAvailability.Disabled(ReaderSharedSettingText { "Unavailable" })
            },
        )

        unavailableSetting.resolveAvailability()

        preference.get() shouldBe true
    }

    private fun registry(
        capabilities: Set<ReaderCapabilityId>,
        declaredSettings: List<ReaderSharedToggleSetting> = listOf(setting),
    ) = ReaderSharedSettingsRegistry(
        listOf(
            object : ReaderSharedSettingsProvider {
                override val potentialCapabilities = capabilities
                override val potentialCapabilitiesBySettingsSurface = mapOf(
                    capableSurfaceId to capabilities,
                    secondCapableSurfaceId to capabilities,
                    unsupportedSurfaceId to setOf(StandardReaderCapabilities.StableTextSelection),
                )
                override val settings = declaredSettings
            },
        ),
    )
}

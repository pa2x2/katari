package mihon.gradle.tasks

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntryViewerSettingsProjectionBoundaryRulesTest {

    @Test
    fun `production resolver must match the declared screen projections`() {
        val omitted = check(registry = "listOf(SettingsReaderScreen)")

        assertEquals(1, omitted.size)
        omitted.single().reason shouldContain "missing from the production resolver: SettingsPlayerScreen"

        val inconsistent = check(
            registry = "listOf(SettingsReaderScreen, SettingsReaderScreen, SettingsPlayerScreen, SettingsGhostScreen)",
        )

        assertEquals(2, inconsistent.size)
        inconsistent.joinToString { finding -> finding.reason } shouldContain "registered more than once"
        inconsistent.joinToString { finding -> finding.reason } shouldContain "no screen projection declaration"
    }

    private fun check(
        registry: String,
        appModule: String = "productionEntryViewerSettingsScreenProjectionResolver()",
    ): List<EntryViewerSettingsProjectionBoundaryFinding> {
        return checkEntryViewerSettingsProjectionBoundaries(
            listOf(
                EntryViewerSettingsProjectionBoundarySource(
                    "app/src/main/java/screens/SettingsReaderScreen.kt",
                    "object SettingsReaderScreen : AppEntryViewerSettingsScreenProjection",
                ),
                EntryViewerSettingsProjectionBoundarySource(
                    "app/src/main/java/screens/SettingsPlayerScreen.kt",
                    "object SettingsPlayerScreen : AppEntryViewerSettingsScreenProjection",
                ),
                EntryViewerSettingsProjectionBoundarySource(
                    "app/src/main/java/eu/kanade/presentation/more/settings/screen/" +
                        "EntryViewerSettingsScreenProjections.kt",
                    registry,
                ),
                EntryViewerSettingsProjectionBoundarySource(
                    "app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt",
                    appModule,
                ),
            ),
        )
    }
}

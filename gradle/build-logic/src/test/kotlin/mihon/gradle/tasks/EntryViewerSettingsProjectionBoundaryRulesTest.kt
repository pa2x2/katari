package mihon.gradle.tasks

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntryViewerSettingsProjectionBoundaryRulesTest {

    @Test
    fun `production resolver installs every declared screen projection`() {
        check(
            registry = "listOf(SettingsReaderScreen, SettingsPlayerScreen)",
        ).shouldBeEmpty()
    }

    @Test
    fun `declared screen projection cannot be omitted`() {
        val findings = check(registry = "listOf(SettingsReaderScreen)")

        assertEquals(1, findings.size)
        findings.single().reason shouldContain "missing from the production resolver: SettingsPlayerScreen"
    }

    @Test
    fun `production resolver cannot duplicate or invent screen projections`() {
        val findings = check(
            registry = "listOf(SettingsReaderScreen, SettingsReaderScreen, SettingsPlayerScreen, SettingsGhostScreen)",
        )

        assertEquals(2, findings.size)
        findings.joinToString { finding -> finding.reason } shouldContain "registered more than once"
        findings.joinToString { finding -> finding.reason } shouldContain "no screen projection declaration"
    }

    @Test
    fun `AppModule must install the enforced production resolver`() {
        val findings = check(
            registry = "listOf(SettingsReaderScreen, SettingsPlayerScreen)",
            appModule = "viewerSettingsScreenProjectionResolver = customResolver",
        )

        assertEquals(1, findings.size)
        findings.single().reason shouldContain "AppModule must install"
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

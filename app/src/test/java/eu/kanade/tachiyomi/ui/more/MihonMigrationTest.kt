package eu.kanade.tachiyomi.ui.more

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MihonMigrationTest {

    @Test
    fun `offers migration only during fresh onboarding when Mihon is installed`() {
        shouldOfferMihonMigration(
            onboardingComplete = false,
            migrationPromptHandled = false,
            mihonInstalled = true,
        ) shouldBe true
    }

    @Test
    fun `does not offer migration when Mihon is absent`() {
        shouldOfferMihonMigration(
            onboardingComplete = false,
            migrationPromptHandled = false,
            mihonInstalled = false,
        ) shouldBe false
    }

    @Test
    fun `does not repeat a handled migration prompt`() {
        shouldOfferMihonMigration(
            onboardingComplete = false,
            migrationPromptHandled = true,
            mihonInstalled = true,
        ) shouldBe false
    }

    @Test
    fun `does not offer migration after onboarding`() {
        shouldOfferMihonMigration(
            onboardingComplete = true,
            migrationPromptHandled = false,
            mihonInstalled = true,
        ) shouldBe false
    }

    @Test
    fun `detects released Mihon FOSS package`() {
        resolveInstalledMihonPackage { it == "app.mihon.foss" } shouldBe "app.mihon.foss"
    }

    @Test
    fun `prefers stable Mihon package when multiple variants are installed`() {
        resolveInstalledMihonPackage { it == "app.mihon" || it == "app.mihon.foss" } shouldBe "app.mihon"
    }
}

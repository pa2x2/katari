package mihon.translation.ui.picker

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.model.TranslationModelDescriptor
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.ui.picker.engine.TranslationEngineStatusExplanation
import mihon.translation.ui.picker.engine.TranslationEngineStatusLabel
import mihon.translation.ui.picker.engine.isTranslationEngineSelectionMissing
import mihon.translation.ui.picker.engine.projectTranslationEngineCard
import org.junit.jupiter.api.Test

class TranslationEngineStatusPresentationTest {
    @Test
    fun `every typed status projects to its own textual category`() {
        val statuses = listOf(
            TranslationEngineStatus.Checking to TranslationEngineStatusLabel.Checking,
            TranslationEngineStatus.Ready to TranslationEngineStatusLabel.Ready,
            TranslationEngineStatus.NotInstalled to TranslationEngineStatusLabel.NotInstalled,
            TranslationEngineStatus.ConfigurationRequired("Configure it") to
                TranslationEngineStatusLabel.ConfigurationRequired,
            TranslationEngineStatus.ProviderDisclosureRequired(DISCLOSURE) to
                TranslationEngineStatusLabel.ProviderDisclosureRequired,
            TranslationEngineStatus.ModelDownloadRequired(listOf(MODEL)) to
                TranslationEngineStatusLabel.ModelDownloadRequired,
            TranslationEngineStatus.SystemSetupRequired(TranslationSystemSetupReason.ServiceDisabled) to
                TranslationEngineStatusLabel.SystemSetupRequired,
            TranslationEngineStatus.SetupInProgress() to TranslationEngineStatusLabel.SetupInProgress,
            TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing) to
                TranslationEngineStatusLabel.Unavailable,
        )

        statuses.forEach { (status, expectedLabel) ->
            card(status).status.label shouldBe expectedLabel
        }
    }

    @Test
    fun `only ready cards can be selected`() {
        card(TranslationEngineStatus.Ready).selectable shouldBe true
        card(TranslationEngineStatus.Checking).selectable shouldBe false
        card(TranslationEngineStatus.NotInstalled).selectable shouldBe false
        card(
            TranslationEngineStatus.ConfigurationRequired("Configure it"),
        ).selectable shouldBe false
        card(
            TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing),
        ).selectable shouldBe false
    }

    @Test
    fun `status explanations retain the typed source`() {
        card(TranslationEngineStatus.Checking).status.explanation shouldBe null
        card(TranslationEngineStatus.Ready).status.explanation shouldBe null
        card(TranslationEngineStatus.NotInstalled).status.explanation shouldBe
            TranslationEngineStatusExplanation.InstallProvider
        card(TranslationEngineStatus.ConfigurationRequired("Configure it")).status.explanation shouldBe
            TranslationEngineStatusExplanation.ProviderText("Configure it")
        card(TranslationEngineStatus.ProviderDisclosureRequired(DISCLOSURE)).status.explanation shouldBe
            TranslationEngineStatusExplanation.ProviderDisclosure
        card(TranslationEngineStatus.ModelDownloadRequired(listOf(MODEL))).status.explanation shouldBe
            TranslationEngineStatusExplanation.LanguageData
        card(
            TranslationEngineStatus.SystemSetupRequired(TranslationSystemSetupReason.LanguageModelsRequired),
        ).status.explanation shouldBe TranslationEngineStatusExplanation.SystemSetup(
            TranslationSystemSetupReason.LanguageModelsRequired,
        )
        card(TranslationEngineStatus.SetupInProgress()).status.explanation shouldBe null
        card(
            TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing),
        ).status.explanation shouldBe TranslationEngineStatusExplanation.Unavailable(
            reason = TranslationUnavailableReason.ServiceMissing,
            buildReason = null,
        )
    }

    @Test
    fun `unavailable selected engine remains visibly selected`() {
        card(
            status = TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing),
            selected = ENGINE_ID,
        ).selected shouldBe true
    }

    @Test
    fun `unconfigured profile leaves every card unselected without a missing warning`() {
        val state = state(TranslationEngineStatus.Unavailable(TranslationUnavailableReason.ServiceMissing))

        projectTranslationEngineCard(state, selectedEngine = null).selected shouldBe false
        isTranslationEngineSelectionMissing(listOf(state), selectedEngine = null) shouldBe false
    }

    @Test
    fun `provider reasons and recovery actions remain attached to their card`() {
        val reason = "Enable the local API"
        val model = card(
            status = TranslationEngineStatus.ConfigurationRequired(reason),
            action = TranslationEngineAction.Configure,
        )

        model.action shouldBe TranslationEngineAction.Configure
        model.status.explanation shouldBe TranslationEngineStatusExplanation.ProviderText(reason)
    }

    @Test
    fun `every recovery action is preserved without changing selection`() {
        TranslationEngineAction.entries.forEach { action ->
            val model = card(
                status = TranslationEngineStatus.NotInstalled,
                action = action,
            )

            model.action shouldBe action
            model.selected shouldBe false
        }
    }

    @Test
    fun `unknown stored selection is reported without replacement`() {
        isTranslationEngineSelectionMissing(
            states = listOf(state(TranslationEngineStatus.Ready)),
            selectedEngine = TranslationEngineId("missing"),
        ) shouldBe true
        isTranslationEngineSelectionMissing(
            states = listOf(state(TranslationEngineStatus.Ready)),
            selectedEngine = ENGINE_ID,
        ) shouldBe false
    }

    private fun card(
        status: TranslationEngineStatus,
        selected: TranslationEngineId? = TranslationEngineId("other"),
        action: TranslationEngineAction? = null,
    ) = projectTranslationEngineCard(state(status, action), selected)

    private fun state(
        status: TranslationEngineStatus,
        action: TranslationEngineAction? = null,
    ) = TranslationEngineState(
        engine = ENGINE,
        presentation = null,
        status = status,
        action = action,
    )

    private companion object {
        val ENGINE_ID = TranslationEngineId("test-engine")
        val ENGINE = KnownTranslationEngine(
            id = ENGINE_ID,
            providerId = TranslationProviderId("test-provider"),
            providerName = "Test provider",
            engineName = "Test engine",
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(1),
            details = TranslationEngineDetails(
                description = "Description",
                processingLocation = "Processing",
                privacyDescription = "Privacy",
            ),
        )
        val DISCLOSURE = TranslationProviderDisclosure(
            title = "Disclosure",
            message = "Disclosure message",
            confirmationLabel = "Confirm",
        )
        val MODEL = TranslationModelDescriptor(
            id = TranslationModelId("test-model"),
            language = LanguageTag.require("en"),
            displayName = "English",
        )
    }
}

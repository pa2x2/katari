package mihon.translation.ui.presentation

import android.R
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.KnownTranslationEngine
import mihon.translation.api.engine.TranslationEngineArtwork
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineDetails
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.provider.TranslationResultAttribution
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionResult
import mihon.translation.ui.session.TranslationSessionState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

@RunWith(AndroidJUnit4::class)
class TranslationSessionOverlayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successful_translation_uses_anchored_popup() {
        val state = success(
            translatedText = "Witaj świecie",
            anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
            presentation = DOCUMENTED_PRESENTATION,
        )

        render(state)

        composeRule.onNodeWithText("Witaj świecie").assertIsDisplayed()
        composeRule.onNodeWithText(DOCUMENTED_PRESENTATION.resultAttribution!!.label).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            composeRule.activity.stringResource(MR.strings.action_learn_more),
        ).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription(
            composeRule.activity.stringResource(MR.strings.action_learn_more),
        ).assertCountEquals(0)
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(
            composeRule.activity.stringResource(MR.strings.action_expand),
        ).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.copy),
        )
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.label_more),
        )
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun successful_translation_opens_the_session_engine_chooser() {
        var externalAction: TranslationSessionExternalAction? = null
        render(
            state = success(
                translatedText = "Witaj świecie",
                anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
            ),
            onExternalAction = { externalAction = it },
        )

        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.label_more),
        ).performClick()
        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_choose_engine),
        ).performClick()

        composeRule.runOnIdle {
            assertTrue(externalAction == TranslationSessionExternalAction.ChooseEngine)
        }
    }

    @Test
    fun settling_request_immediately_uses_an_anchored_loading_popup() {
        render(
            TranslationSessionState.Settling(
                input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
            ),
        )

        composeRule.onNodeWithTag(TRANSLATION_SESSION_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
    }

    @Test
    fun anchored_popup_stays_visible_when_translation_state_changes() {
        val anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f)
        var state by mutableStateOf<TranslationSessionState>(
            TranslationSessionState.Settling(input(anchor)),
        )
        render(stateProvider = { state })
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()

        composeRule.runOnIdle {
            state = success(
                translatedText = "Witaj świecie",
                anchor = anchor,
            )
        }

        composeRule.onNodeWithText("Witaj świecie").assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun anchored_popup_hides_offscreen_and_returns_with_its_selection() {
        val visibleAnchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f)
        var state by mutableStateOf<TranslationSessionState>(
            success(
                translatedText = "Witaj świecie",
                anchor = visibleAnchor,
            ),
        )
        render(stateProvider = { state })
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()

        composeRule.runOnIdle {
            state = success(
                translatedText = "Witaj świecie",
                anchor = TranslationSelectionAnchor(400f, -100f, 680f, -40f),
            )
        }

        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assert(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility),
        )
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)

        composeRule.runOnIdle {
            state = success(
                translatedText = "Witaj świecie",
                anchor = visibleAnchor,
            )
        }

        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun failed_translation_with_a_valid_anchor_uses_the_shared_popup_content() {
        render(
            TranslationSessionState.Failed(
                input = input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
                failure = TranslationSessionFailure.UnexpectedExecutionFailure,
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_failed),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun documented_provider_disclosure_keeps_guidance_in_compact_chrome() {
        val disclosure = TranslationProviderDisclosure(
            title = "Use Offline Translator",
            message = "Katari sends selected text to Offline Translator over 127.0.0.1. " +
                "Translation remains on this device. Keep the provider HTTP API bound to localhost.",
            confirmationLabel = "Allow on-device translation",
            documentationUrl = DOCUMENTED_PRESENTATION.documentationUrl,
        )
        render(
            TranslationSessionState.PreparationRequired(
                input = input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
                preparation = TranslationPreparation.ProviderDisclosureRequired(
                    engine = TranslationEngineId("offline-translator"),
                    presentation = DOCUMENTED_PRESENTATION,
                    disclosure = disclosure,
                ),
            ),
        )

        composeRule.onNodeWithText(disclosure.title).assertIsDisplayed()
        composeRule.onNodeWithText(disclosure.confirmationLabel)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(36.dp)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.action_learn_more),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.action_expand),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            composeRule.activity.stringResource(MR.strings.action_learn_more),
        ).assertCountEquals(0)
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun compact_engine_recovery_delegates_the_unbounded_catalog_to_the_picker() {
        val engines = List(6) { index -> engine(index) }
        render(
            TranslationSessionState.PreparationRequired(
                input = input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
                preparation = TranslationPreparation.EngineChoiceRequired(
                    reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(
                        TranslationEngineId("missing-engine"),
                    ),
                    engines = engines,
                ),
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_choose_engine),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Translation engine", substring = true).assertCountEquals(0)
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
    }

    @Test
    fun embedded_first_loading_keeps_a_visible_padded_container() {
        composeRule.setContent {
            MaterialTheme {
                TranslationSessionContent(
                    state = TranslationSessionState.Settling(input(anchor = null)),
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
                    showEngineChange = false,
                    showCopy = true,
                    useExternalEnginePicker = true,
                    onDismiss = {},
                    onExecute = {},
                    onRetry = {},
                    onCopy = {},
                    onExpand = {},
                    onSelectSource = {},
                    onSelectEngine = {},
                    onExternalAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(TRANSLATION_SESSION_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_CONTENT_TAG).assertHeightIsAtLeast(40.dp)
    }

    @Test
    fun embedded_progress_keeps_the_previous_result_and_its_actions() {
        val previousSuccess = success(
            translatedText = "Previous translation",
            anchor = null,
        )
        val previousResult = TranslationSessionResult(
            input = previousSuccess.input,
            result = previousSuccess.result,
        )
        val state = TranslationSessionState.Translating(
            input = input(anchor = null),
            presentation = PRESENTATION,
            previousResult = previousResult,
        )

        composeRule.setContent {
            MaterialTheme {
                TranslationSessionContent(
                    state = state,
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
                    showEngineChange = false,
                    showCopy = true,
                    useExternalEnginePicker = true,
                    onDismiss = {},
                    onExecute = {},
                    onRetry = {},
                    onCopy = {},
                    onExpand = {},
                    onSelectSource = {},
                    onSelectEngine = {},
                    onExternalAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(TRANSLATION_SESSION_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Previous translation").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.copy),
        ).assertIsDisplayed()
    }

    @Test
    fun long_success_stays_in_a_compact_anchored_popup_with_expansion() {
        val state = success(
            translatedText = List(80) { "A translated line that must be measured." }.joinToString("\n"),
            anchor = TranslationSelectionAnchor(400f, 1100f, 680f, 1160f),
        )

        render(state)

        val popup = composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        assertTrue(
            popup.boundsInRoot.height <= 360 * composeRule.activity.resources.displayMetrics.density,
        )
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.stringResource(MR.strings.action_expand),
        ).assertIsDisplayed()
    }

    @Test
    fun page_spanning_selection_uses_adaptive_sheet_without_leaving_a_popup() {
        val displayMetrics = composeRule.activity.resources.displayMetrics
        val anchorMargin = 160f
        val anchor = TranslationSelectionAnchor(
            left = anchorMargin,
            top = anchorMargin,
            right = displayMetrics.widthPixels - anchorMargin,
            bottom = displayMetrics.heightPixels - anchorMargin,
        )
        var state by mutableStateOf<TranslationSessionState>(
            TranslationSessionState.Settling(input(anchor)),
        )

        render(stateProvider = { state })

        composeRule.onNodeWithTag(TRANSLATION_SESSION_SHEET_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_POPUP_TAG).assertCountEquals(0)

        composeRule.runOnIdle {
            state = success(
                translatedText = "Witaj świecie",
                anchor = anchor,
            )
        }

        composeRule.onNodeWithText("Witaj świecie").assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_SHEET_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_POPUP_TAG).assertCountEquals(0)
    }

    @Test
    fun missing_anchor_and_language_choice_use_adaptive_sheet() {
        val state = TranslationSessionState.PreparationRequired(
            input = input(anchor = null),
            preparation = TranslationPreparation.SourceUndetermined(listOf(SOURCE)),
        )

        render(state)

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_source_undetermined),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun unsupported_language_pair_opens_existing_language_correction_instead_of_retrying() {
        var externalAction: TranslationSessionExternalAction? = null
        render(
            state = TranslationSessionState.PreparationRequired(
                input = input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
                preparation = TranslationPreparation.Unavailable(
                    TranslationUnavailableReason.UnsupportedLanguagePair(CATALAN, TARGET),
                ),
            ),
            onExternalAction = { externalAction = it },
        )

        composeRule.onAllNodesWithText(
            composeRule.activity.stringResource(MR.strings.action_retry),
        ).assertCountEquals(0)
        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_change_languages),
        )
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(
                externalAction == TranslationSessionExternalAction.ChangeLanguages(CATALAN, TARGET),
            )
        }
    }

    @Test
    fun non_language_unavailability_retains_retry_recovery() {
        render(
            TranslationSessionState.PreparationRequired(
                input = input(
                    anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f),
                ),
                preparation = TranslationPreparation.Unavailable(
                    TranslationUnavailableReason.ServiceMissing,
                ),
            ),
        )

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.action_retry),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            composeRule.activity.stringResource(MR.strings.translation_change_languages),
        ).assertCountEquals(0)
    }

    @Test
    fun embedded_content_reuses_setup_renderer_without_overlay_chrome() {
        val state = TranslationSessionState.PreparationRequired(
            input = input(anchor = null),
            preparation = TranslationPreparation.SystemSetupRequired(
                engine = TranslationEngineId("android-system"),
                presentation = PRESENTATION,
                reason = TranslationSystemSetupReason.LanguageModelsRequired,
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                TranslationSessionContent(
                    state = state,
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
                    showEngineChange = false,
                    showCopy = true,
                    useExternalEnginePicker = true,
                    onDismiss = {},
                    onExecute = {},
                    onRetry = {},
                    onCopy = {},
                    onExpand = {},
                    onSelectSource = {},
                    onSelectEngine = {},
                    onExternalAction = {},
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_system_setup_required),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_SHEET_TAG).assertCountEquals(0)
        composeRule.onAllNodesWithTag(TRANSLATION_SESSION_POPUP_TAG).assertCountEquals(0)
    }

    @Test
    fun setup_progress_does_not_offer_a_manual_retry() {
        val state = TranslationSessionState.PreparationRequired(
            input = input(anchor = null),
            preparation = TranslationPreparation.SetupInProgress(
                engine = TranslationEngineId("android-system"),
                presentation = PRESENTATION,
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                TranslationSessionContent(
                    state = state,
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
                    showEngineChange = false,
                    showCopy = true,
                    useExternalEnginePicker = true,
                    onDismiss = {},
                    onExecute = {},
                    onRetry = {},
                    onCopy = {},
                    onExpand = {},
                    onSelectSource = {},
                    onSelectEngine = {},
                    onExternalAction = {},
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.stringResource(MR.strings.translation_setup_in_progress),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText(
            composeRule.activity.stringResource(MR.strings.action_retry),
        ).assertCountEquals(0)
    }

    private fun render(
        state: TranslationSessionState,
        onExternalAction: (TranslationSessionExternalAction) -> Unit = {},
    ) {
        render(stateProvider = { state }, onExternalAction = onExternalAction)
    }

    private fun render(
        stateProvider: () -> TranslationSessionState,
        onExternalAction: (TranslationSessionExternalAction) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    TranslationSessionOverlay(
                        state = stateProvider(),
                        expanded = false,
                        isTabletUi = false,
                        onDismiss = {},
                        onExecute = {},
                        onRetry = {},
                        onCopy = {},
                        onExpand = {},
                        onSelectSource = {},
                        onSelectEngine = {},
                        onExternalAction = onExternalAction,
                    )
                }
            }
        }
    }

    private companion object {
        val SOURCE = LanguageTag.require("en")
        val TARGET = LanguageTag.require("pl")
        val CATALAN = LanguageTag.require("ca")
        val PROVIDER = TranslationProviderId("android")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = PROVIDER,
            providerName = "Android",
            engineName = "System on-device translation",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
        )
        val DOCUMENTED_PRESENTATION = TranslationProviderPresentation(
            providerId = TranslationProviderId("offline-translator"),
            providerName = "Offline Translator",
            engineName = "Offline Translator",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
            resultAttribution = TranslationResultAttribution("Offline Translator"),
            documentationUrl = "https://example.com/offline-translator",
        )

        fun engine(index: Int) = KnownTranslationEngine(
            id = TranslationEngineId("engine-$index"),
            providerId = TranslationProviderId("provider-$index"),
            providerName = "Provider $index",
            engineName = "Translation engine $index",
            buildAvailability = TranslationEngineBuildAvailability.Included,
            artwork = TranslationEngineArtwork.Bundled(R.drawable.ic_menu_view),
            details = TranslationEngineDetails(
                description = "Description",
                processingLocation = "This device",
                privacyDescription = "Private",
            ),
        )

        fun input(anchor: TranslationSelectionAnchor?): TranslationSessionInput {
            return TranslationSessionInput(
                request = TranslationRequest(
                    text = "Hello world",
                    sourceLanguage = TranslationSourceLanguageSelection.Explicit(SOURCE),
                    targetLanguage = TranslationTargetLanguageSelection.Explicit(TARGET),
                ),
                anchor = anchor,
            )
        }

        fun success(
            translatedText: String,
            anchor: TranslationSelectionAnchor?,
            presentation: TranslationProviderPresentation = PRESENTATION,
        ): TranslationSessionState.Success {
            return TranslationSessionState.Success(
                input = input(anchor),
                result = TranslationResult(
                    translatedText = translatedText,
                    sourceLanguage = SOURCE,
                    targetLanguage = TARGET,
                    presentation = presentation,
                ),
            )
        }
    }
}

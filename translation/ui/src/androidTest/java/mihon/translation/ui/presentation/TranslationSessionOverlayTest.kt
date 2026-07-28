package mihon.translation.ui.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.translation.api.TranslationInvocationPolicy
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationProviderId
import mihon.translation.api.TranslationProviderPresentation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationResult
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionFailure
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
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
        )

        render(state)

        composeRule.onNodeWithText("Witaj świecie").assertIsDisplayed()
        composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG).assertIsDisplayed()
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
    fun embedded_first_loading_keeps_a_visible_padded_container() {
        composeRule.setContent {
            MaterialTheme {
                TranslationSessionContent(
                    state = TranslationSessionState.Settling(input(anchor = null)),
                    expanded = true,
                    showHeader = false,
                    showExpand = false,
                    showLanguageChange = false,
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
        val previousResult = success(
            translatedText = "Previous translation",
            anchor = null,
        ).result
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
    fun measured_popup_overflow_promotes_to_adaptive_sheet() {
        val state = success(
            translatedText = List(80) { "A translated line that must be measured." }.joinToString("\n"),
            anchor = TranslationSelectionAnchor(400f, 1100f, 680f, 1160f),
        )

        render(state)

        composeRule.onNodeWithTag(TRANSLATION_SESSION_SHEET_TAG).assertIsDisplayed()
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
    fun embedded_content_reuses_setup_renderer_without_overlay_chrome() {
        val state = TranslationSessionState.PreparationRequired(
            input = input(anchor = null),
            preparation = TranslationPreparation.SystemSetupRequired(
                engine = mihon.translation.api.TranslationEngineId("android-system"),
                presentation = PRESENTATION,
                reason = mihon.translation.api.TranslationSystemSetupReason.LanguageModelsRequired,
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
                engine = mihon.translation.api.TranslationEngineId("android-system"),
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

    private fun render(state: TranslationSessionState) {
        composeRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    TranslationSessionOverlay(
                        state = state,
                        expanded = false,
                        isTabletUi = false,
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
        }
    }

    private companion object {
        val SOURCE = TranslationLanguageTag.require("en")
        val TARGET = TranslationLanguageTag.require("pl")
        val PROVIDER = TranslationProviderId("android")
        val PRESENTATION = TranslationProviderPresentation(
            providerId = PROVIDER,
            providerName = "Android",
            engineName = "System on-device translation",
            invocationPolicy = TranslationInvocationPolicy.Immediate,
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
        ): TranslationSessionState.Success {
            return TranslationSessionState.Success(
                input = input(anchor),
                result = TranslationResult(
                    translatedText = translatedText,
                    sourceLanguage = SOURCE,
                    targetLanguage = TARGET,
                    presentation = PRESENTATION,
                ),
            )
        }
    }
}

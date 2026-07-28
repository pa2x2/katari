package mihon.translation.ui.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

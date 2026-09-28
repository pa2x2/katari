package mihon.translation.ui.presentation

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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationProviderId
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.api.provider.TranslationProviderPresentation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.api.result.TranslationResult
import mihon.translation.ui.session.TranslationSelectionAnchor
import mihon.translation.ui.session.TranslationSessionInput
import mihon.translation.ui.session.TranslationSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

@RunWith(AndroidJUnit4::class)
class TranslationSessionOverlayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun anchored_popup_keeps_its_width_when_translation_state_changes() {
        val anchor = TranslationSelectionAnchor(400f, 280f, 680f, 340f)
        var state by mutableStateOf<TranslationSessionState>(
            TranslationSessionState.Settling(input(anchor)),
        )
        render(stateProvider = { state })
        val loadingWidth = composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .width

        composeRule.runOnIdle {
            state = success(
                translatedText = "Witaj świecie",
                anchor = anchor,
            )
        }

        composeRule.onNodeWithText("Witaj świecie").assertIsDisplayed()
        val resultWidth = composeRule.onNodeWithTag(TRANSLATION_SESSION_POPUP_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .width
        assertEquals(loadingWidth, resultWidth, 0.5f)
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
    fun long_success_stays_in_a_compact_anchored_popup_with_expansion() {
        val state = success(
            translatedText = List(80) { "A translated line that must be measured." }.joinToString("\n"),
            anchor = TranslationSelectionAnchor(400f, 1100f, 680f, 1160f),
        )

        render(stateProvider = { state })

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

    private fun render(stateProvider: () -> TranslationSessionState) {
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
                        onExternalAction = {},
                        onSelectTarget = {},
                        languageSuggestions = null,
                    )
                }
            }
        }
    }

    private companion object {
        val SOURCE = LanguageTag.require("en")
        val TARGET = LanguageTag.require("pl")
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

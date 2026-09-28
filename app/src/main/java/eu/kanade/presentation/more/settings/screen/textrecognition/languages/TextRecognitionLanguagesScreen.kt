package eu.kanade.presentation.more.settings.screen.textrecognition.languages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.textrecognition.presentation.selectionLabel
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.languages.TextRecognitionLanguagesList
import mihon.text.recognition.ui.languages.textRecognitionLanguageOverview
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/** How the profile's draft reads every language, where a language's pipeline is chosen or its choice removed. */
internal class TextRecognitionLanguagesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()
        val hostActions = model.hostActions
        val overview = remember(state.draft) {
            textRecognitionLanguageOverview(state.languages, state.draft) { hostActions.resolve(state.draft, it) }
        }
        val rowModels = remember(overview) {
            val choices = overview.choices.mapNotNull { choice ->
                choice.pipeline?.let { hostActions.models(it, choice.language) }
            }
            val groups = (overview.engineGroups + overview.otherEngineGroups)
                .map { hostActions.models(it.preset.pipeline, it.languages.first()) }
            (choices + groups).flatten().distinct()
        }
        val modelStates by remember(rowModels) { model.controller.observeModels(rowModels) }
            .collectAsState(initial = emptyMap())
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_languages),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            TextRecognitionLanguagesList(
                overview = overview,
                providers = state.providers,
                selectionLabel = { hostActions.selectionLabel(it) },
                models = hostActions::models,
                modelStates = modelStates,
                onChooseLanguage = { navigator.push(TextRecognitionOverridePipelineScreen(it.value)) },
                onChooseInGroup = { languages ->
                    navigator.push(TextRecognitionOverrideLanguageScreen(languages.map { it.value }))
                },
                onRemoveChoice = { choice ->
                    model.controller.removeDraftOverride(choice.language)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = context.stringResource(
                                MR.strings.text_recognition_languages_removed,
                                choice.language.displayName(),
                            ),
                            actionLabel = context.stringResource(MR.strings.action_undo),
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            model.controller.setDraftOverride(choice.language, choice.selection)
                        }
                    }
                },
                onAddChoice = { navigator.push(TextRecognitionOverrideLanguageScreen()) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

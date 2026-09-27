package eu.kanade.presentation.more.settings.screen.textrecognition.overrides

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.textrecognition.presentation.selectionLabel
import eu.kanade.presentation.util.Screen
import mihon.text.recognition.ui.overrides.TextRecognitionOverridesList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

internal class TextRecognitionOverridesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val state by model.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_language_overrides),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            TextRecognitionOverridesList(
                overrides = state.draft.overrides,
                selectionLabel = { model.hostActions.selectionLabel(it) },
                onEdit = { language -> navigator.push(TextRecognitionOverridePipelineScreen(language.value)) },
                onDelete = model.controller::removeDraftOverride,
                onAdd = { navigator.push(TextRecognitionOverrideLanguageScreen()) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

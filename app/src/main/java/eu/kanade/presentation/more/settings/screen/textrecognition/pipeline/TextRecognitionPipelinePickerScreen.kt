package eu.kanade.presentation.more.settings.screen.textrecognition.pipeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import mihon.language.api.tag.LanguageTag

internal class TextRecognitionPipelinePickerScreen(
    private val languageTag: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { TextRecognitionPipelinePickerScreenModel(LanguageTag.require(languageTag)) }
        val state by model.state.collectAsState()

        TextRecognitionPipelinePickerContent(
            state = state,
            onSelect = model::select,
            onDownload = model::download,
            onCancelDownload = model::cancel,
            onBack = navigator::pop,
        )
    }
}

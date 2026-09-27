package mihon.translation.ui.presentation.language

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.displayName
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Announces in [hostState] that the content now keeps its own target language, offering to make it the default for
 * all content.
 */
@Composable
internal fun TranslationKeptTargetSnackbar(
    coordinator: TranslationSessionHostCoordinator,
    hostState: SnackbarHostState,
) {
    var keptTarget by remember(coordinator) { mutableStateOf<LanguageTag?>(null) }
    LaunchedEffect(coordinator) {
        coordinator.newlyKeptTargets.collect { keptTarget = it }
    }
    val language = keptTarget ?: return
    val message = stringResource(MR.strings.translation_language_series_target_chosen, language.displayName())
    val actionLabel = stringResource(MR.strings.translation_language_use_for_all)
    LaunchedEffect(language, hostState) {
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) coordinator.makeDefaultTarget(language)
        keptTarget = null
    }
}

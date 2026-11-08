package app.katari.translation.ui.presentation.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.feedback.SnackbarDuration
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.presentation.kit.feedback.SnackbarResult
import app.katari.translation.ui.picker.language.displayName
import app.katari.translation.ui.session.TranslationSessionHostCoordinator
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

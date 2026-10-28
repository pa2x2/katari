package mihon.translation.ui.presentation.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.TextRole
import mihon.translation.api.provider.TranslationInvocationPolicy
import mihon.translation.ui.session.TranslationSessionState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The engine that will translate and the action that starts it. */
@Composable
internal fun ReadyContent(
    state: TranslationSessionState.Ready,
    onExecute: () -> Unit,
    compact: Boolean,
) {
    Text(
        text = state.preparation.presentation.engineName,
        role = TextRole.Label,
        maxLines = if (compact) 1 else Int.MAX_VALUE,
        overflow = if (compact) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
    val policy = state.preparation.presentation.invocationPolicy
    SessionActionButton(
        label = when (policy) {
            TranslationInvocationPolicy.Immediate -> stringResource(MR.strings.action_translate)
            is TranslationInvocationPolicy.ExplicitAction -> policy.label
        },
        onClick = onExecute,
    )
}

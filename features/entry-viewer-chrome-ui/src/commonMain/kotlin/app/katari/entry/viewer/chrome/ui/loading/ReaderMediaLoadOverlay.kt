package app.katari.entry.viewer.chrome.ui.loading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.progress.CombinedCircularProgressIndicator
import app.katari.presentation.kit.surface.OverlayPanel
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

sealed interface ReaderMediaLoadState {
    data object Ready : ReaderMediaLoadState
    data class Loading(val progress: Float? = null) : ReaderMediaLoadState
    data class Failed(val message: String) : ReaderMediaLoadState
}

/** A way past media that failed, offered under Retry, such as the immersive feed's next title. */
class ReaderMediaSkip(val label: String, val onSkip: () -> Unit)

/** Next title, where the immersive feed's media fails, so a title that can't play doesn't hold the feed on it. */
@Composable
fun nextTitleSkip(onNextTitle: (() -> Unit)?): ReaderMediaSkip? =
    onNextTitle?.let { ReaderMediaSkip(stringResource(MR.strings.browse_immersive_next), it) }

@Composable
fun ReaderMediaLoadOverlay(
    state: ReaderMediaLoadState,
    modifier: Modifier = Modifier,
    previewModel: Any? = null,
    showBackground: Boolean = true,
    onBackgroundClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    skip: ReaderMediaSkip? = null,
) {
    if (state == ReaderMediaLoadState.Ready) return

    Box(modifier = modifier) {
        if (showBackground) {
            ReaderMediaLoadingBackground(
                previewModel = previewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
        onBackgroundClick?.let { onClick ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
        when (state) {
            ReaderMediaLoadState.Ready -> Unit
            is ReaderMediaLoadState.Loading -> CombinedCircularProgressIndicator(
                progress = { state.progress ?: 0f },
                modifier = Modifier.align(Alignment.Center),
            )
            is ReaderMediaLoadState.Failed -> {
                OverlayPanel(
                    modifier = Modifier.align(Alignment.Center).padding(KatariTheme.spacing.xl),
                    style = OverlayStyle.Solid,
                ) {
                    Column(
                        modifier = Modifier.padding(KatariTheme.spacing.l),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message, role = TextRole.Body)
                        onRetry?.let { retry ->
                            Button(
                                label = stringResource(MR.strings.action_retry),
                                onClick = retry,
                                modifier = Modifier.padding(top = KatariTheme.spacing.m),
                            )
                        }
                        skip?.let { skip ->
                            Button(
                                label = skip.label,
                                onClick = skip.onSkip,
                                modifier = Modifier.padding(top = KatariTheme.spacing.s),
                                style = ButtonStyle.Tonal,
                            )
                        }
                    }
                }
            }
        }
    }
}

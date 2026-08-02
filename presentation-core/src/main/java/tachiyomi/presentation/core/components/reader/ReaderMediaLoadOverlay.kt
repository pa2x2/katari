package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CombinedCircularProgressIndicator
import tachiyomi.presentation.core.i18n.stringResource

sealed interface ReaderMediaLoadState {
    data object Ready : ReaderMediaLoadState
    data class Loading(val progress: Float? = null) : ReaderMediaLoadState
    data class Failed(val message: String) : ReaderMediaLoadState
}

@Composable
fun ReaderMediaLoadOverlay(
    state: ReaderMediaLoadState,
    modifier: Modifier = Modifier,
    previewModel: Any? = null,
    showBackground: Boolean = true,
    onBackgroundClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    if (state == ReaderMediaLoadState.Ready) return

    val loading = state is ReaderMediaLoadState.Loading
    var showLoadingIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(LOADING_INDICATOR_DELAY_MS)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

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
            is ReaderMediaLoadState.Loading -> {
                if (showLoadingIndicator) {
                    CombinedCircularProgressIndicator(
                        progress = { state.progress ?: 0f },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            is ReaderMediaLoadState.Failed -> {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                        onRetry?.let { retry ->
                            Button(onClick = retry, modifier = Modifier.padding(top = 12.dp)) {
                                Text(stringResource(MR.strings.action_retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val LOADING_INDICATOR_DELAY_MS = 250L

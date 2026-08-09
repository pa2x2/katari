package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import kotlinx.coroutines.delay
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.ui.settings.TtsSettingsState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TtsSettingsContent(
    state: TtsSettingsState,
    searchHighlightKey: String?,
    onSearchHighlightConsumed: (String) -> Unit,
    onBack: (() -> Unit)?,
    onChooseEngine: () -> Unit,
    onChooseDefaultVoice: () -> Unit,
    onChooseVoiceOverrides: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    configurationReady: Boolean,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val listState = rememberLazyListState()
    val searchTargets = setOf(
        stringResource(MR.strings.tts_settings_playground),
        stringResource(MR.strings.tts_settings_engine),
        stringResource(MR.strings.tts_settings_default_voice),
        stringResource(MR.strings.tts_settings_language_overrides),
        stringResource(MR.strings.tts_settings_pitch),
        stringResource(MR.strings.tts_settings_preview),
    )
    val highlightPlayground = searchHighlightKey != null && searchHighlightKey in searchTargets
    LaunchedEffect(searchHighlightKey, highlightPlayground) {
        val key = searchHighlightKey ?: return@LaunchedEffect
        if (highlightPlayground) {
            delay(SEARCH_HIGHLIGHT_SCROLL_DELAY)
            listState.animateScrollToItem(PLAYGROUND_ITEM_INDEX)
        }
        onSearchHighlightConsumed(key)
    }

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.tts_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            item {
                TtsSettingsPlayground(
                    state = state,
                    configurationReady = configurationReady,
                    highlighted = highlightPlayground,
                    onChooseEngine = onChooseEngine,
                    onChooseDefaultVoice = onChooseDefaultVoice,
                    onChooseVoiceOverrides = onChooseVoiceOverrides,
                    onPitchChange = onPitchChange,
                    onTogglePreview = onTogglePreview,
                    onSave = onSave,
                    onAcknowledgeDisclosure = onAcknowledgeDisclosure,
                    onOpenSetup = onOpenSetup,
                )
            }
        }
    }
}

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds

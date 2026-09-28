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
import eu.kanade.presentation.more.settings.widget.draft.SettingsDraftSaveBar
import eu.kanade.presentation.more.settings.widget.draft.rememberSettingsDraftLeaveGuard
import kotlinx.coroutines.delay
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.ui.settings.TtsSettingsState
import tachiyomi.i18n.*
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
    onDiscard: () -> Unit,
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
    val saveEnabled = state.hasUnsavedProfileChanges && configurationReady
    val navigateUp = rememberSettingsDraftLeaveGuard(
        hasUnsavedChanges = state.hasUnsavedProfileChanges,
        saveEnabled = saveEnabled,
        onSave = onSave,
        onDiscard = onDiscard,
        onLeave = onBack,
    )

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.tts_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack?.let { navigateUp },
                scrollBehavior = it,
            )
        },
        bottomBar = {
            SettingsDraftSaveBar(
                visible = state.hasUnsavedProfileChanges,
                saveEnabled = saveEnabled,
                onDiscard = onDiscard,
                onSave = onSave,
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
                    onAcknowledgeDisclosure = onAcknowledgeDisclosure,
                    onOpenSetup = onOpenSetup,
                )
            }
        }
    }
}

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds

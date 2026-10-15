package eu.kanade.presentation.more.settings.screen.translation.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.GuideButton
import eu.kanade.presentation.more.settings.screen.translation.TranslationPlaygroundState
import eu.kanade.presentation.more.settings.screen.translation.series.SeriesTranslationLanguagesEntry
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.more.settings.widget.draft.SettingsDraftSaveBar
import eu.kanade.presentation.more.settings.widget.draft.rememberSettingsDraftLeaveGuard
import kotlinx.coroutines.delay
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.pulsingHighlight
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.ui.picker.language.supportsPair
import mihon.translation.ui.presentation.TranslationSessionExternalAction
import mihon.translation.ui.session.TranslationLanguageSupportState
import mihon.translation.ui.session.TranslationSessionController
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TranslationSettingsContent(
    playground: TranslationPlaygroundState,
    engines: List<TranslationEngineState>,
    languageSupport: TranslationLanguageSupportState,
    controller: TranslationSessionController,
    searchHighlightKey: String?,
    onSearchHighlightConsumed: (String) -> Unit,
    onBack: (() -> Unit)?,
    onTextChange: (String) -> Unit,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapLanguages: () -> Unit,
    onChooseEngine: () -> Unit,
    canOpenSetup: Boolean,
    onOpenSetup: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onExternalAction: (TranslationSessionExternalAction) -> Unit,
    seriesLanguageCount: Int?,
    onOpenSeriesLanguages: () -> Unit,
) {
    val playgroundTitle = stringResource(MR.strings.translation_settings_playground)
    val engineTitle = stringResource(MR.strings.translation_settings_engine)
    val targetTitle = stringResource(MR.strings.translation_settings_target)
    val listState = rememberLazyListState()
    val seriesLanguagesTitle = stringResource(MR.strings.translation_series_languages)
    val highlightPlayground = searchHighlightKey == playgroundTitle ||
        searchHighlightKey == engineTitle ||
        searchHighlightKey == targetTitle
    LaunchedEffect(searchHighlightKey, highlightPlayground) {
        val key = searchHighlightKey ?: return@LaunchedEffect
        if (highlightPlayground) {
            delay(SEARCH_HIGHLIGHT_SCROLL_DELAY)
            listState.animateScrollToItem(PLAYGROUND_ITEM_INDEX)
        }
        onSearchHighlightConsumed(key)
    }
    val saveEnabled = playground.hasUnsavedProfileChanges &&
        (languageSupport as? TranslationLanguageSupportState.Available)
            ?.takeIf { it.engine == playground.engine }
            ?.support
            ?.supportsPair(playground.sourceLanguage, playground.targetLanguage) == true
    val navigateUp = rememberSettingsDraftLeaveGuard(
        hasUnsavedChanges = playground.hasUnsavedProfileChanges,
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
                        title = stringResource(MR.strings.translation_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack?.let { navigateUp },
                actions = { GuideButton(page = "translation/") },
                scrollBehavior = it,
            )
        },
        bottomBar = {
            SettingsDraftSaveBar(
                visible = playground.hasUnsavedProfileChanges,
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
        ) {
            item {
                SeriesTranslationLanguagesEntry(
                    count = seriesLanguageCount,
                    onClick = onOpenSeriesLanguages,
                    modifier = Modifier.pulsingHighlight(Unit.takeIf { searchHighlightKey == seriesLanguagesTitle }),
                )
            }
            item { SectionHeader(playgroundTitle) }
            item {
                TranslationPlaygroundCard(
                    state = playground,
                    engines = engines,
                    languageSupport = languageSupport,
                    controller = controller,
                    onTextChange = onTextChange,
                    onChooseSource = onChooseSource,
                    onChooseTarget = onChooseTarget,
                    onSwapLanguages = onSwapLanguages,
                    onChooseEngine = onChooseEngine,
                    canOpenSetup = canOpenSetup,
                    onOpenSetup = onOpenSetup,
                    onExternalAction = onExternalAction,
                    highlighted = highlightPlayground,
                )
            }
        }
    }
}

// The playground section header, so the scroll shows it above the card.
private const val PLAYGROUND_ITEM_INDEX = 1
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds

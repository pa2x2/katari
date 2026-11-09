package app.katari.desktop.reader

import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.reader.window.MangaReaderNavigation
import app.katari.language.api.tag.LanguageTag
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.screen.textrecognition.languages.TextRecognitionOverridePipelineScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination

/**
 * Where [screen]'s reader leaves for: its settings, the settings its text features need and the web view open over it,
 * as Android's open over the reader's activity, and the entry takes its place, as Android's reader gives way to the
 * main screen showing it.
 */
internal class WindowReaderNavigation(
    private val navigator: Navigator,
    private val screen: MangaReaderScreen,
    private val webViewDestination: WebViewDestination,
) : MangaReaderNavigation {
    override val webViewAvailable: Boolean get() = webViewDestination.isAvailable

    override fun close() {
        if (navigator.lastItem === screen) navigator.pop()
    }

    override fun openReaderSettings() {
        navigator.push(
            SettingsScreen(
                destination = SettingsScreen.Destination.Readers,
                viewerSettingsSurfaceId = MangaReaderSettings.PROVIDER_ID,
                viewerSettingsHighlight = null,
            ),
        )
    }

    override fun openTextRecognitionPipelineChoice(language: LanguageTag) {
        navigator.push(TextRecognitionOverridePipelineScreen(language.value, storeOnConfirm = true))
    }

    override fun openTranslationSettings() {
        navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
    }

    override fun openEntry(entryId: Long) {
        if (navigator.lastItem ===
            screen
        ) {
            navigator.replace(EntryScreen(entryId))
        } else {
            navigator.push(EntryScreen(entryId))
        }
    }

    override fun openWebView(url: String, title: String?, sourceId: Long?) {
        navigator.push(webViewDestination.screen(url, title, sourceId, headers = emptyMap()))
    }
}

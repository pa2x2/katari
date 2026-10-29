package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreenDestination
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.WebViewScreenDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val AppLanguageScreenDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val WebViewScreenDestination.webViewDestination: WebViewDestination
}

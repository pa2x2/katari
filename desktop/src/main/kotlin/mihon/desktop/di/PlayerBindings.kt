package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.desktop.player.WindowAnimePlayerDestination
import mihon.entry.interactions.player.AnimePlayer
import mihon.entry.interactions.player.AnimePlayerDestination

@BindingContainer
object PlayerBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun animePlayerDestination(
        player: AnimePlayer,
        webViewDestination: WebViewDestination,
    ): AnimePlayerDestination = WindowAnimePlayerDestination(player, webViewDestination)
}

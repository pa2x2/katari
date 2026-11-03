package eu.kanade.presentation.more.settings.screen.translation

import mihon.translation.api.host.TranslationHostActions

/** Graph entry points of the translation settings rows, which settings search builds without the screen's model. */
interface TranslationSettingsGraph {
    val translationHostActions: TranslationHostActions
}

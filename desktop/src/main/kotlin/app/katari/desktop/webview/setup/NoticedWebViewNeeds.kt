package app.katari.desktop.webview.setup

import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ExtensionPages

/*
 * The pages that open with no screen around them, a challenge's and an extension's own, as their callers ask whether
 * they are offered: asked only as one is needed, so an answer of no is a need the set-up hears of.
 */

/** [pages], telling [onMissing] each time a challenge found none offered. */
class NoticedChallengePage(private val pages: ChallengePage, private val onMissing: () -> Unit) :
    ChallengePage by pages {
    override val isOffered: Boolean
        get() = pages.isOffered.also { offered -> if (!offered) onMissing() }
}

/** [pages], telling [onMissing] each time an extension's web view found none offered. */
class NoticedExtensionPages(private val pages: ExtensionPages, private val onMissing: () -> Unit) :
    ExtensionPages by pages {
    override val isOffered: Boolean
        get() = pages.isOffered.also { offered -> if (!offered) onMissing() }
}

package eu.kanade.presentation.more.onboarding

import mihon.core.platform.PlatformContext

/** Mihon installed beside the app, whose library the first-run guide offers to bring over through a backup. */
interface MihonApp {
    /**
     * What tells the installed Mihon apart, such as its Android package name; null where none is installed. It looks
     * the app up, so remember the answer.
     */
    fun findInstalled(): String?

    /** Opens the installed Mihon [id], for the user to create a backup in. False when it couldn't be opened. */
    fun open(context: PlatformContext, id: String): Boolean
}

package eu.kanade.tachiyomi.ui.more

import eu.kanade.presentation.util.Screen

/** The first-run guide, which the advanced settings open again on request. */
interface OnboardingDestination {
    /** False where the platform doesn't host the guide yet; the row for it is then hidden. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}

package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import mihon.presentation.kit.feedback.GuidePageHeading

/** One page of the first-run guide: what its heading says, whether it lets the guide go on, and what it shows. */
internal interface OnboardingStep {

    @Composable
    fun heading(): GuidePageHeading

    /** Whether Next is available. */
    val isComplete: Boolean

    /** Whether the page offers Skip beside Next, for a page that can be left undone. */
    val canSkip: Boolean
        get() = false

    @Composable
    fun Content()
}

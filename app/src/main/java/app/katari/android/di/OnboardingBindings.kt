package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.onboarding.AndroidOnboardingPermissions
import eu.kanade.presentation.more.onboarding.OnboardingPermissions

@BindingContainer
interface OnboardingBindings {
    @Binds
    val AndroidOnboardingPermissions.onboardingPermissions: OnboardingPermissions
}

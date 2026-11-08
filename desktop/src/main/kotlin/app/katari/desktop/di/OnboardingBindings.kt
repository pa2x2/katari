package app.katari.desktop.di

import app.katari.desktop.onboarding.NoMihonApp
import app.katari.desktop.onboarding.NoOnboardingPermissions
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.onboarding.MihonApp
import eu.kanade.presentation.more.onboarding.OnboardingPermissions

@BindingContainer
interface OnboardingBindings {
    @Binds
    val NoOnboardingPermissions.onboardingPermissions: OnboardingPermissions

    @Binds
    val NoMihonApp.mihonApp: MihonApp
}

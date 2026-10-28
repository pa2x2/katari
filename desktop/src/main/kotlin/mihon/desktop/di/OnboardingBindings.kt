package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.onboarding.MihonApp
import eu.kanade.presentation.more.onboarding.OnboardingPermissions
import mihon.desktop.onboarding.NoMihonApp
import mihon.desktop.onboarding.NoOnboardingPermissions

@BindingContainer
interface OnboardingBindings {
    @Binds
    val NoOnboardingPermissions.onboardingPermissions: OnboardingPermissions

    @Binds
    val NoMihonApp.mihonApp: MihonApp
}

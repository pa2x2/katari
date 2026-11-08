package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.onboarding.AndroidOnboardingPermissions
import eu.kanade.presentation.more.onboarding.MihonApp
import eu.kanade.presentation.more.onboarding.OnboardingPermissions
import eu.kanade.tachiyomi.ui.more.AndroidMihonApp

@BindingContainer
interface OnboardingBindings {
    @Binds
    val AndroidOnboardingPermissions.onboardingPermissions: OnboardingPermissions

    @Binds
    val AndroidMihonApp.mihonApp: MihonApp
}

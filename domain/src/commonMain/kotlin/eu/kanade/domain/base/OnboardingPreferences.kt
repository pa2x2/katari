package eu.kanade.domain.base

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** How far the first-run guide got. Kept on the base store, so the guide runs once for every profile. */
class OnboardingPreferences(preferenceStore: PreferenceStore) {
    val shownOnboardingFlow: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("onboarding_complete"),
        false,
    )

    /** Whether the guide's offer to bring the library over from Mihon was taken up or passed over. */
    val mihonMigrationPromptHandled: Preference<Boolean> = preferenceStore.getBoolean(
        Preference.appStateKey("mihon_migration_prompt_handled"),
        false,
    )
}

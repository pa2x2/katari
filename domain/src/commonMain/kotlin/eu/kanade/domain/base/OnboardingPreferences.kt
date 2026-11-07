package eu.kanade.domain.base

import eu.kanade.domain.ui.UiPreferences
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

    /**
     * The person-level settings the guide's own pages set, as the app names them, while it hasn't finished. Sync's first
     * sync doesn't count them as the person's data, so picking light or dark on the way to linking doesn't stop the join
     * to ask whose settings win; the account's then replace them.
     */
    fun settingsChosenInGuide(): Set<String> = if (shownOnboardingFlow.get()) emptySet() else GUIDE_SETTING_KEYS

    private companion object {
        val GUIDE_SETTING_KEYS = setOf(UiPreferences.THEME_MODE_KEY)
    }
}

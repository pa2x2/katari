package eu.kanade.presentation.more.settings.screen.security

import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences

/** Graph entry points of the security settings. */
interface SecuritySettingsGraph {
    val securityPreferences: SecurityPreferences
    val privacyPreferences: PrivacyPreferences
    val secureScreen: SecureScreen
}

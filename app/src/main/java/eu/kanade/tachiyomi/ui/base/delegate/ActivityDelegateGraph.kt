package eu.kanade.tachiyomi.ui.base.delegate

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences

interface ActivityDelegateGraph {
    val basePreferences: BasePreferences
    val securityPreferences: SecurityPreferences
    val uiPreferences: UiPreferences
}

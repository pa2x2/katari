package tachiyomi.presentation.widget

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import tachiyomi.domain.updates.interactor.GetUpdates

/** Graph entry points of the updates widgets, which the launcher's widget host constructs. */
@ContributesTo(AppScope::class)
interface UpdatesWidgetGraph {
    val getUpdates: GetUpdates
    val securityPreferences: SecurityPreferences
}

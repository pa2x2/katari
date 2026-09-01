package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun UpdatesBadge() {
    val count by produceState(initialValue = 0) {
        val preferences = Injekt.get<LibraryPreferences>()
        combine(
            preferences.newShowUpdatesCount.changes(),
            preferences.newUpdatesCount.changes(),
        ) { show, updates -> if (show) updates else 0 }
            .collectLatest { value = it }
    }
    if (count > 0) {
        Badge {
            val description = pluralStringResource(
                MR.plurals.notification_updates_generic,
                count = count,
                count,
            )
            Text(
                text = count.toString(),
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

@Composable
internal fun BrowseBadge() {
    val count by produceState(initialValue = 0) {
        val extensionManager = Injekt.get<ExtensionManager>()
        combine(
            extensionManager.pendingUpdatesCount,
            extensionManager.isAutoUpdateInProgress,
        ) { updates, inProgress -> if (inProgress) 0 else updates }
            .collectLatest { value = it }
    }
    if (count > 0) {
        Badge {
            val description = pluralStringResource(
                MR.plurals.update_check_notification_ext_updates,
                count = count,
                count,
            )
            Text(
                text = count.toString(),
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}

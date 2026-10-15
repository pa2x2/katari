package mihon.translation.provider.server.setup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

sealed interface ProviderSetupStatus {
    data object Testing : ProviderSetupStatus

    data class Success(
        val message: String,
    ) : ProviderSetupStatus

    data class Failure(
        val message: String,
    ) : ProviderSetupStatus
}

/** The outcome of testing a provider's connection, or that the test is still running. */
@Composable
fun ProviderSetupStatusPanel(
    status: ProviderSetupStatus,
    testingLabel: String,
    successLabel: String,
    failureLabel: String,
) {
    val heading = when (status) {
        ProviderSetupStatus.Testing -> testingLabel
        is ProviderSetupStatus.Success -> successLabel
        is ProviderSetupStatus.Failure -> failureLabel
    }
    val message = when (status) {
        ProviderSetupStatus.Testing -> null
        is ProviderSetupStatus.Success -> status.message
        is ProviderSetupStatus.Failure -> status.message
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        container = when (status) {
            ProviderSetupStatus.Testing -> SurfaceContainer.Highest
            is ProviderSetupStatus.Success -> SurfaceContainer.TertiaryContainer
            is ProviderSetupStatus.Failure -> SurfaceContainer.ErrorContainer
        },
        shape = KatariTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.l),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status == ProviderSetupStatus.Testing) {
                LoadingIndicator(modifier = Modifier.size(KatariTheme.sizes.icon))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                Text(text = heading, role = TextRole.SectionTitle)
                message?.let { Text(text = it, role = TextRole.Body) }
            }
        }
    }
}

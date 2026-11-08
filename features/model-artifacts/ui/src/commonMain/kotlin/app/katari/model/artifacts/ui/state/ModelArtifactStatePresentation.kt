package app.katari.model.artifacts.ui.state

import androidx.compose.runtime.Composable
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.model.artifacts.api.state.ModelArtifactFailure
import app.katari.model.artifacts.api.state.ModelArtifactState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One-line description of where [descriptor] stands, including its size. */
@Composable
fun modelArtifactStateLabel(descriptor: ModelArtifactDescriptor, state: ModelArtifactState): String = when (state) {
    ModelArtifactState.NotInstalled -> stringResource(
        MR.strings.model_artifacts_state_not_installed,
        formatModelArtifactSize(descriptor.sizeBytes),
    )
    is ModelArtifactState.Downloading -> stringResource(
        MR.strings.model_artifacts_state_downloading,
        formatModelArtifactSize(state.downloadedBytes),
        formatModelArtifactSize(state.totalBytes),
    )
    is ModelArtifactState.Installed -> stringResource(
        MR.strings.model_artifacts_state_installed,
        formatModelArtifactSize(descriptor.sizeBytes),
    )
    is ModelArtifactState.Failed -> modelArtifactFailureLabel(state.failure)
}

@Composable
fun modelArtifactFailureLabel(failure: ModelArtifactFailure): String = when (failure) {
    ModelArtifactFailure.MeteredNetwork -> stringResource(MR.strings.model_artifacts_failure_metered)
    is ModelArtifactFailure.InsufficientStorage -> stringResource(
        MR.strings.model_artifacts_failure_storage_space,
        formatModelArtifactSize(failure.requiredBytes),
        formatModelArtifactSize(failure.availableBytes),
    )
    is ModelArtifactFailure.ChecksumMismatch -> stringResource(MR.strings.model_artifacts_failure_checksum)
    is ModelArtifactFailure.Network -> stringResource(
        MR.strings.model_artifacts_failure_network,
        failure.message ?: stringResource(MR.strings.model_artifacts_failure_unknown),
    )
    is ModelArtifactFailure.Storage -> stringResource(
        MR.strings.model_artifacts_failure_storage,
        failure.message ?: stringResource(MR.strings.model_artifacts_failure_unknown),
    )
}

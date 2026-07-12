package mihon.feature.profiles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.hasNameConflict
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ProfilesSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val profileManager = remember { Injekt.get<ProfileManager>() }

        val profiles by profileManager.profiles.collectAsState()
        val activeProfile by profileManager.activeProfile.collectAsState()
        val visibleProfiles = remember(profiles) { profiles.filterNot(Profile::isArchived) }
        val archivedProfiles = remember(profiles) { profiles.filter(Profile::isArchived) }

        var dialog by remember { mutableStateOf<ProfilesDialog?>(null) }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.profiles_title),
                    navigateUp = navigator::pop,
                    actions = {
                        IconButton(onClick = { dialog = ProfilesDialog.Create }) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(MR.strings.profiles_add_profile),
                            )
                        }
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionHeader(
                        title = stringResource(MR.strings.profiles_visible),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(visibleProfiles, key = Profile::id) { profile ->
                    ProfileCard(
                        profile = profile,
                        isActive = activeProfile?.id == profile.id,
                        onRename = { dialog = ProfilesDialog.Rename(profile) },
                        onArchive = { dialog = ProfilesDialog.Archive(profile) },
                        onDelete = null,
                    )
                }
                if (archivedProfiles.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(MR.strings.profiles_archived))
                    }
                    items(archivedProfiles, key = Profile::id) { profile ->
                        ProfileCard(
                            profile = profile,
                            isActive = activeProfile?.id == profile.id,
                            onRename = { dialog = ProfilesDialog.Rename(profile) },
                            onArchive = {
                                scope.launch {
                                    profileManager.setProfileArchived(profile.id, archived = false)
                                }
                            },
                            onDelete = { dialog = ProfilesDialog.Delete(profile) },
                        )
                    }
                }
            }
        }

        when (val currentDialog = dialog) {
            null -> Unit
            ProfilesDialog.Create -> {
                ProfileNameDialog(
                    title = stringResource(MR.strings.profiles_add_profile),
                    initialValue = "",
                    existingProfiles = profiles,
                    onDismissRequest = { dialog = null },
                    onConfirm = { name ->
                        scope.launch {
                            runCatching {
                                profileManager.createProfile(name)
                            }.onSuccess {
                                dialog = null
                            }
                        }
                    },
                )
            }
            is ProfilesDialog.Rename -> {
                ProfileNameDialog(
                    title = stringResource(MR.strings.profiles_rename_profile),
                    initialValue = currentDialog.profile.name,
                    existingProfiles = profiles,
                    originalProfileId = currentDialog.profile.id,
                    onDismissRequest = { dialog = null },
                    onConfirm = { name ->
                        scope.launch {
                            runCatching {
                                profileManager.renameProfile(currentDialog.profile.id, name)
                            }.onSuccess {
                                dialog = null
                            }
                        }
                    },
                )
            }
            is ProfilesDialog.Archive -> {
                ConfirmProfileDialog(
                    title = stringResource(MR.strings.profiles_archive_profile),
                    message = stringResource(
                        MR.strings.profiles_archive_profile_confirmation,
                        currentDialog.profile.name,
                    ),
                    confirmLabel = stringResource(MR.strings.action_archive),
                    onDismissRequest = { dialog = null },
                    onConfirm = {
                        scope.launch {
                            profileManager.setProfileArchived(currentDialog.profile.id, archived = true)
                            dialog = null
                        }
                    },
                )
            }
            is ProfilesDialog.Delete -> {
                ConfirmProfileDialog(
                    title = stringResource(MR.strings.profiles_delete_profile),
                    message = stringResource(
                        MR.strings.profiles_delete_profile_confirmation,
                        currentDialog.profile.name,
                    ),
                    confirmLabel = stringResource(MR.strings.action_delete),
                    onDismissRequest = { dialog = null },
                    onConfirm = {
                        scope.launch {
                            profileManager.permanentlyDeleteProfile(currentDialog.profile.id)
                            dialog = null
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = profile.name, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    StatusLabel(stringResource(MR.strings.profiles_active))
                }
                if (profile.id == ProfileConstants.DEFAULT_PROFILE_ID) {
                    StatusLabel(stringResource(MR.strings.profiles_default))
                }
                if (profile.isArchived) {
                    StatusLabel(stringResource(MR.strings.profiles_archived))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (profile.isArchived) {
                    TextButton(onClick = onArchive) {
                        Text(stringResource(MR.strings.action_restore))
                    }
                }
                TextButton(onClick = onRename) {
                    Text(stringResource(MR.strings.action_edit))
                }
                if (!profile.isArchived && !isActive && profile.id != ProfileConstants.DEFAULT_PROFILE_ID) {
                    TextButton(onClick = onArchive) {
                        Text(stringResource(MR.strings.action_archive))
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(MR.strings.action_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ProfileNameDialog(
    title: String,
    initialValue: String,
    existingProfiles: List<Profile>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    originalProfileId: Long? = null,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val trimmedValue = value.trim()
    val duplicate = remember(trimmedValue, existingProfiles, originalProfileId) {
        existingProfiles.hasNameConflict(
            name = trimmedValue,
            excludedProfileId = originalProfileId,
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(MR.strings.name)) },
                    isError = trimmedValue.isNotEmpty() && duplicate,
                    supportingText = {
                        Text(
                            text = if (trimmedValue.isNotEmpty() && duplicate) {
                                stringResource(MR.strings.profiles_name_exists)
                            } else {
                                stringResource(MR.strings.information_required_plain)
                            },
                        )
                    },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmedValue.isNotEmpty() && !duplicate,
                onClick = { onConfirm(trimmedValue) },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun ConfirmProfileDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

private sealed interface ProfilesDialog {
    data object Create : ProfilesDialog
    data class Rename(val profile: Profile) : ProfilesDialog
    data class Archive(val profile: Profile) : ProfilesDialog
    data class Delete(val profile: Profile) : ProfilesDialog
}

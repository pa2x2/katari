package mihon.feature.profiles.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.GuideButton
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.components.ProfileNameDialog
import mihon.feature.profiles.ui.components.ProfileRow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class ProfilesSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val profileManager = remember { context.appGraph.profileManager }

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
                        GuideButton(page = "profiles/")
                        IconButton(
                            icon = MaterialSymbols.Rounded.Add,
                            contentDescription = stringResource(MR.strings.profiles_add_profile),
                            onClick = { dialog = ProfilesDialog.Create },
                        )
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                item {
                    SectionHeader(text = stringResource(MR.strings.profiles_visible))
                }
                items(visibleProfiles, key = Profile::id) { profile ->
                    ProfileRow(
                        profile = profile,
                        isActive = activeProfile?.id == profile.id,
                        onRename = { dialog = ProfilesDialog.Rename(profile) },
                        onArchive = { dialog = ProfilesDialog.Archive(profile) },
                        onDelete = null,
                    )
                }
                if (archivedProfiles.isNotEmpty()) {
                    item {
                        SectionHeader(text = stringResource(MR.strings.profiles_archived))
                    }
                    items(archivedProfiles, key = Profile::id) { profile ->
                        ProfileRow(
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
                ConfirmDialog(
                    title = stringResource(MR.strings.profiles_archive_profile),
                    text = stringResource(
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
                ConfirmDialog(
                    title = stringResource(MR.strings.profiles_delete_profile),
                    text = stringResource(
                        MR.strings.profiles_delete_profile_confirmation,
                        currentDialog.profile.name,
                    ),
                    confirmLabel = stringResource(MR.strings.action_delete),
                    onConfirm = {
                        scope.launch {
                            profileManager.permanentlyDeleteProfile(currentDialog.profile.id)
                            dialog = null
                        }
                    },
                    onDismissRequest = { dialog = null },
                    destructive = true,
                )
            }
        }
    }
}

private sealed interface ProfilesDialog {
    data object Create : ProfilesDialog
    data class Rename(val profile: Profile) : ProfilesDialog
    data class Archive(val profile: Profile) : ProfilesDialog
    data class Delete(val profile: Profile) : ProfilesDialog
}

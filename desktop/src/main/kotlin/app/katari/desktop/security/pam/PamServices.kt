package app.katari.desktop.security.pam

import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * The PAM service the app checks the sign-in with: the first of the distributions' shared sign-in stacks that this
 * system has, since the app can't install a service of its own from its AppImage. `system-auth` is Arch's, Fedora's
 * and their relatives', `common-auth` Debian's, Ubuntu's and openSUSE's; `login`, which every system has, is the last
 * resort. Null where none is configured.
 */
internal fun findPamService(configDirectories: List<Path> = PAM_CONFIG_DIRECTORIES): String? =
    SERVICES.firstOrNull { pamServiceFile(it, configDirectories) != null }

/** The file Linux-PAM reads [service] from, the first of [configDirectories] that has it; null where none does. */
internal fun pamServiceFile(service: String, configDirectories: List<Path>): Path? =
    configDirectories.map { it.resolve(service) }.firstOrNull { it.isRegularFile() }

private val SERVICES = listOf("system-auth", "common-auth", "login")

// Where Linux-PAM reads services: the administrator's, then the distribution's (openSUSE keeps its in /usr/etc)
internal val PAM_CONFIG_DIRECTORIES = listOf("/etc/pam.d", "/usr/lib/pam.d", "/usr/etc/pam.d").map(Path::of)

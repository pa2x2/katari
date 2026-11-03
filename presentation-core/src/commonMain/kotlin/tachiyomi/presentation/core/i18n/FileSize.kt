package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable

/** [bytes] as a size for the user, such as "12.34 MB". */
@Composable
expect fun fileSize(bytes: Long): String

/** [bytes] as a size for the user with fewer digits, such as "12 MB". */
@Composable
expect fun shortFileSize(bytes: Long): String

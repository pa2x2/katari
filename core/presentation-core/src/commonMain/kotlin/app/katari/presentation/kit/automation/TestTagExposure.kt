package app.katari.presentation.kit.automation

import androidx.compose.ui.Modifier

/** Lets UI automation outside the app find this subtree's test tags, where the platform needs telling. */
internal expect fun Modifier.exposeTestTags(): Modifier

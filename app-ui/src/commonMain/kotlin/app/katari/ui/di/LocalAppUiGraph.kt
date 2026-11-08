package app.katari.ui.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppUiGraph: ProvidableCompositionLocal<AppUiGraph> = staticCompositionLocalOf {
    error("No AppUiGraph provided around this composition")
}

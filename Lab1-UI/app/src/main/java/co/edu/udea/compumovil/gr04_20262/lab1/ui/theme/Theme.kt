package co.edu.udea.compumovil.gr04_20262.lab1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = md_primary,
    onPrimary = md_onPrimary,
    primaryContainer = md_primaryContainer,
    onPrimaryContainer = md_onPrimaryContainer,
    secondary = md_secondary,
    onSecondary = md_onSecondary,
    secondaryContainer = md_secondaryContainer,
    onSecondaryContainer = md_onSecondaryContainer,
    tertiary = md_tertiary,
    onTertiary = md_onTertiary,
    tertiaryContainer = md_tertiaryContainer,
    onTertiaryContainer = md_onTertiaryContainer,
    error = md_error,
    onError = md_onError,
    errorContainer = md_errorContainer,
    onErrorContainer = md_onErrorContainer,
    background = md_background,
    onBackground = md_onBackground,
    surface = md_surface,
    onSurface = md_onSurface,
    surfaceVariant = md_surfaceVariant,
    onSurfaceVariant = md_onSurfaceVariant,
    outline = md_outline,
    surfaceContainer = md_surfaceContainer,
)

private val DarkColors = darkColorScheme(
    primary = md_primary_d,
    onPrimary = md_onPrimary_d,
    primaryContainer = md_primaryContainer_d,
    onPrimaryContainer = md_onPrimaryContainer_d,
    secondary = md_secondary_d,
    onSecondary = md_onSecondary_d,
    secondaryContainer = md_secondaryContainer_d,
    onSecondaryContainer = md_onSecondaryContainer_d,
    tertiary = md_tertiary_d,
    onTertiary = md_onTertiary_d,
    tertiaryContainer = md_tertiaryContainer_d,
    onTertiaryContainer = md_onTertiaryContainer_d,
    error = md_error_d,
    onError = md_onError_d,
    errorContainer = md_errorContainer_d,
    onErrorContainer = md_onErrorContainer_d,
    background = md_background_d,
    onBackground = md_onBackground_d,
    surface = md_surface_d,
    onSurface = md_onSurface_d,
    surfaceVariant = md_surfaceVariant_d,
    onSurfaceVariant = md_onSurfaceVariant_d,
    outline = md_outline_d,
    surfaceContainer = md_surfaceContainer_d,
)

@Composable
fun Lab1UITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

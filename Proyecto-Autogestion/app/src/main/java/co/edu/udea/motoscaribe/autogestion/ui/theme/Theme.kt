package co.edu.udea.motoscaribe.autogestion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta derivada del rojo de marca #DC1C2E (anteproyecto).
private val Light = lightColorScheme(
    primary = Color(0xFFDC1C2E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD8), onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF775653), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD7), onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF735C2E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6), onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF231918),
    surface = Color(0xFFFFF8F7), onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDB), onSurfaceVariant = Color(0xFF534342),
    outline = Color(0xFF857371), outlineVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF0EF),
    surfaceContainer = Color(0xFFFCEAE8), surfaceContainerHigh = Color(0xFFF7E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDC),
    inverseSurface = Color(0xFF392E2D), inverseOnSurface = Color(0xFFFFEDEB),
    inversePrimary = Color(0xFFFFB3B0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB3B0), onPrimary = Color(0xFF680010),
    primaryContainer = Color(0xFF93000F), onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFE7BDB8), onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3C), onSecondaryContainer = Color(0xFFFFDAD7),
    tertiary = Color(0xFFE4C28C), onTertiary = Color(0xFF412D05),
    tertiaryContainer = Color(0xFF5B431A), onTertiaryContainer = Color(0xFFFFDEA6),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF1DFDC),
    surface = Color(0xFF1A1110), onSurface = Color(0xFFF1DFDC),
    surfaceVariant = Color(0xFF534342), onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A), outlineVariant = Color(0xFF534342),
    surfaceContainerLowest = Color(0xFF140C0B), surfaceContainerLow = Color(0xFF231918),
    surfaceContainer = Color(0xFF271D1C), surfaceContainerHigh = Color(0xFF322826),
    surfaceContainerHighest = Color(0xFF3D3231),
    inverseSurface = Color(0xFFF1DFDC), inverseOnSurface = Color(0xFF392E2D),
    inversePrimary = Color(0xFFDC1C2E),
)

@Composable
fun MotosCaribeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}

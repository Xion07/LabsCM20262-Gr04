package co.edu.udea.motoscaribe.autogestion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Rojo de marca tomado del anteproyecto.
private val Brand = Color(0xFFDC1C2E)
private val BrandLight = Color(0xFFF9D3D8)

private val Light = lightColorScheme(
    primary = Brand,
    primaryContainer = BrandLight,
    onPrimaryContainer = Color(0xFF3A0008),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB3B8),
    primaryContainer = Color(0xFF93000F),
)

@Composable
fun MotosCaribeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}

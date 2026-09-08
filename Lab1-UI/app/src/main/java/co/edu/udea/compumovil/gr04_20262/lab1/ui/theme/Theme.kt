package co.edu.udea.compumovil.gr04_20262.lab1.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = IndigoLight,
    background = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    secondary = Indigo
)

@Composable
fun Lab1UITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

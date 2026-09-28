package fr.uge.android.forkeat.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForkEatColorScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary900,
    secondary = Secondary800,
    onSecondary = Color.White,
    secondaryContainer = Secondary100,
    onSecondaryContainer = Secondary900,
    tertiary = Secondary500,
    onTertiary = Color.White,
    background = SurfaceCream,
    onBackground = Secondary900,
    surface = Color.White,
    onSurface = Secondary900,
    surfaceVariant = SurfaceCream,
    onSurfaceVariant = Secondary700,
    outline = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

@Composable
fun ForkEatTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForkEatColorScheme,
        typography = Typography,
        content = content
    )
}

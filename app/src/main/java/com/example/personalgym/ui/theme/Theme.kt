package com.example.personalgym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.personalgym.ui.illustration.FigureColors

val Lime = Color(0xFFC6F432)

/** True when the active colour scheme is the light one. */
val isLightTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

// Accents get deeper shades on light backgrounds so text and bars stay readable.
val Coral: Color
    @Composable @ReadOnlyComposable
    get() = if (isLightTheme) Color(0xFFE0532F) else Color(0xFFFF7A59)
val Sky: Color
    @Composable @ReadOnlyComposable
    get() = if (isLightTheme) Color(0xFF1E88D6) else Color(0xFF5BC0FF)
val Violet: Color
    @Composable @ReadOnlyComposable
    get() = if (isLightTheme) Color(0xFF7A4FD6) else Color(0xFFB28CFF)

private val DarkScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF151A05),
    primaryContainer = Color(0xFF2E3A0C),
    onPrimaryContainer = Lime,
    secondary = Color(0xFFFF7A59),
    onSecondary = Color(0xFF2A0D04),
    secondaryContainer = Color(0xFF3D1F16),
    onSecondaryContainer = Color(0xFFFFC2B1),
    tertiary = Color(0xFF5BC0FF),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFEDEFF2),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFEDEFF2),
    surfaceVariant = Color(0xFF1E222A),
    onSurfaceVariant = Color(0xFFA3A9B4),
    surfaceContainer = Color(0xFF171A20),
    surfaceContainerHigh = Color(0xFF1E222A),
    surfaceContainerHighest = Color(0xFF262B34),
    surfaceContainerLow = Color(0xFF14171C),
    outline = Color(0xFF3A404B),
    outlineVariant = Color(0xFF2A2F38),
    error = Color(0xFFFF5D6C),
)

// Lime is too pale for text on white, so light mode uses a deep olive as primary
// and keeps lime for container fills (selected cards, done sets, the Today card).
private val LightScheme = lightColorScheme(
    primary = Color(0xFF4A6B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF59A),
    onPrimaryContainer = Color(0xFF1E2B00),
    secondary = Color(0xFFC2451F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1D7),
    onSecondaryContainer = Color(0xFF5C1A06),
    tertiary = Color(0xFF1E88D6),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF14171C),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF14171C),
    surfaceVariant = Color(0xFFE6E8EC),
    onSurfaceVariant = Color(0xFF5B6270),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1F4),
    surfaceContainerHighest = Color(0xFFE6E8EC),
    surfaceContainerLow = Color.White,
    outline = Color(0xFFC3C8D0),
    outlineVariant = Color(0xFFDDE0E5),
    error = Color(0xFFC62838),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val StatNumber = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)

@Composable
fun PersonalGymTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = AppTypography, content = content)
}

@Composable
fun figureColors() = if (isLightTheme) {
    FigureColors(
        body = Color(0xFF1E222A),
        bodyFar = Color(0xFF9097A3),
        equipment = Color(0xFFE0532F),
        accent = Color(0xFF6E9A00),
        floor = Color(0xFFC3C8D0),
        outline = Color(0xFFE6E8EC),
    )
} else {
    FigureColors(
        body = Color(0xFFEDEFF2),
        bodyFar = Color(0xFF7D8490),
        equipment = Color(0xFFFF7A59),
        accent = Lime,
        floor = Color(0xFF3A404B),
        outline = Color(0xFF1B1F26),
    )
}

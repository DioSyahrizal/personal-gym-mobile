package com.example.personalgym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.personalgym.ui.illustration.FigureColors

val Lime = Color(0xFFC6F432)
val Coral = Color(0xFFFF7A59)
val Sky = Color(0xFF5BC0FF)
val Violet = Color(0xFFB28CFF)

private val Scheme = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF151A05),
    primaryContainer = Color(0xFF2E3A0C),
    onPrimaryContainer = Lime,
    secondary = Coral,
    onSecondary = Color(0xFF2A0D04),
    secondaryContainer = Color(0xFF3D1F16),
    onSecondaryContainer = Color(0xFFFFC2B1),
    tertiary = Sky,
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
fun PersonalGymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}

@Composable
fun figureColors() = FigureColors(
    body = Color(0xFFEDEFF2),
    bodyFar = Color(0xFF7D8490),
    equipment = Coral,
    accent = Lime,
    floor = Color(0xFF3A404B),
    outline = Color(0xFF1B1F26),
)

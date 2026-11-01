package com.chelita.almuerzos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta completa de la app: tonos cálidos (naranja), por ser de comida.
// Se definen todos los colores para que Material no use sus morados por defecto.
private val Colores = lightColorScheme(
    primary = Color(0xFFBF4A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF8A4F2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3D3),
    onSecondaryContainer = Color(0xFF4A1E05),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF231A15),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF231A15),
    surfaceVariant = Color(0xFFF5DED3),
    onSurfaceVariant = Color(0xFF53443C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF6F1),
    surfaceContainer = Color(0xFFFFF0E8),
    surfaceContainerHigh = Color(0xFFFCE9DF),
    surfaceContainerHighest = Color(0xFFF6E3D9),
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD8C2B8),
)

// Envuelve cualquier pantalla para aplicarle los colores y tipografías de la app.
@Composable
fun AlmuerzosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, content = content)
}

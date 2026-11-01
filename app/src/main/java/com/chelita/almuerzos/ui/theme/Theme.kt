package com.chelita.almuerzos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores de la app. Naranja como color principal, por ser de comida.
private val Colores = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
)

// Envuelve cualquier pantalla para aplicarle los colores y tipografías de la app.
@Composable
fun AlmuerzosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, content = content)
}

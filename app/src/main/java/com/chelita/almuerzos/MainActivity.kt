package com.chelita.almuerzos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.chelita.almuerzos.ui.theme.AlmuerzosTheme

// Punto de entrada: Android crea esta Activity al abrir la app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ajusta la barra de estado (hora, batería) al fondo claro de la app.
        enableEdgeToEdge()
        // setContent dibuja la interfaz usando Jetpack Compose.
        setContent {
            AlmuerzosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaBienvenida()
                }
            }
        }
    }
}

// Una función @Composable describe un pedazo de pantalla.
@Composable
fun PantallaBienvenida() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hola", style = MaterialTheme.typography.displayMedium)
        Text("Almuerzos Chelita", style = MaterialTheme.typography.titleLarge)
    }
}

// @Preview permite ver la pantalla en Android Studio sin ejecutar la app.
@Preview(showBackground = true)
@Composable
fun PantallaBienvenidaPreview() {
    AlmuerzosTheme { PantallaBienvenida() }
}

package com.chelita.almuerzos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chelita.almuerzos.ui.Navegacion
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
                Navegacion()
            }
        }
    }
}

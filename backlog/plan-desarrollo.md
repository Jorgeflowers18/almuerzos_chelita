# Plan de desarrollo — Almuerzos Chelita

App Android nativa (Kotlin + Jetpack Compose + Room) para que las meseras registren los pedidos del almuerzo y calculen los cobros, incluidas las tarrinas para llevar. Funciona sin internet y se instala como APK firmada, sin Play Store.

| Fase | Qué se hace | Qué podrás probar | Estado |
|---|---|---|---|
| **1. Base** | Crear el proyecto, configurarlo y hacer una pantalla "Hola" | Compilar y verla en el emulador o en tu celular | Hecha |
| **2. Lógica de precios** | El cálculo de totales y tarrinas, con **pruebas automáticas** | Ejecutar las pruebas y confirmar que los cálculos salen bien | Hecha |
| **3. Base de datos** | Pedidos, productos y precios guardados con Room (SQLite) | Que los datos sigan ahí después de cerrar la app | Hecha |
| **4. Pedidos** | Lista del día, nuevo pedido con + y −, editar pedido y cambiar estados | El flujo completo de una mesera | Pendiente |
| **5. Despacho y cobro** | Lo que falta servir y el desglose para cobrar | | Pendiente |
| **6. Resumen** | Totales del día, del mes actual y del mes anterior | | Pendiente |
| **7. Administración** | Precios, menú del día y limpieza de datos de más de 2 meses | | Pendiente |
| **8. Respaldos** | Respaldo automático diario, exportar/importar y copia en Google Drive | Pasar los datos a otro celular | Pendiente |
| **9. Publicación** | Crear la clave de firma, generar la APK firmada e instalarla | La APK final en el celular de ellas | Pendiente |

Al terminar cada fase hacemos un commit, para tener siempre una versión que funcione.

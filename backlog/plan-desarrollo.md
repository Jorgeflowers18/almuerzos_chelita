# Plan de desarrollo — Almuerzos Chelita

App Android nativa (Kotlin + Jetpack Compose + Room) para que las meseras registren los pedidos del almuerzo y calculen los cobros, incluidas las tarrinas para llevar. Funciona sin internet y se instala como APK firmada, sin Play Store.

| Fase | Qué se hace | Qué podrás probar | Estado |
|---|---|---|---|
| **1. Base** | Crear el proyecto, configurarlo y hacer una pantalla "Hola" | Compilar y verla en el emulador o en tu celular | Hecha |
| **2. Lógica de precios** | El cálculo de totales y tarrinas, con **pruebas automáticas** | Ejecutar las pruebas y confirmar que los cálculos salen bien | Hecha |
| **3. Base de datos** | Pedidos, productos y precios guardados con Room (SQLite) | Que los datos sigan ahí después de cerrar la app | Hecha |
| **4. Pedidos** | Lista del día, nuevo pedido con + y −, editar pedido y cambiar estados | El flujo completo de una mesera | Hecha |
| **5. Despacho y cobro** | Lo que falta servir y el desglose para cobrar | Ver qué preparar en "Por servir", cobrar con cálculo de vuelto y enviar la cuenta por WhatsApp | Hecha |
| **6. Resumen** | Totales del día, del mes actual y del mes anterior | Ver lo vendido, lo cobrado y lo que falta cobrar, por producto y día por día | Hecha |
| **7. Administración** | Precios, menú del día y limpieza de datos de más de 2 meses | Cambiar precios, anotar el menú y borrar pedidos antiguos desde "Ajustes" | Hecha |
| **8. Respaldos** | Respaldo automático en una carpeta del celular (un solo archivo que se reemplaza), respaldo antes de limpiar, enviar (WhatsApp/Drive) y restaurar | Pasar los datos a otro celular, y recuperarlos tras desinstalar la app | Hecha |
| **9. Publicación** | Crear la clave de firma, generar la APK firmada e instalarla | La APK final en el celular de ellas (ver `docs/publicacion.md`) | Hecha |

Al terminar cada fase hacemos un commit, para tener siempre una versión que funcione.

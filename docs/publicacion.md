# Publicación de Almuerzos Chelita

Cómo se genera la APK, cómo se instala en el celular del restaurante y cómo se entregan versiones nuevas sin perder datos.

## Dónde está cada cosa

| Qué | Dónde | ¿En git? |
|---|---|---|
| Clave de firma | `C:\Users\JorgeFlores\Documents\AlmuerzosChelita\firma\almuerzos-chelita.jks` | No, nunca |
| Contraseña de la clave | `...\AlmuerzosChelita\firma\keystore.properties` (y una copia en la raíz del proyecto, ignorada por git) | No, nunca |
| APK lista para entregar | `C:\Users\JorgeFlores\Documents\AlmuerzosChelita\apk\` | No |

## Lo más importante: la clave de firma

Android solo acepta una actualización si viene firmada **con la misma clave** que la versión instalada. Si la clave se pierde, la única forma de instalar una versión nueva es desinstalar la app, y al desinstalar **se borran los datos** del celular.

Haz ya mismo una copia de **toda la carpeta `firma`** (el `.jks` y el `keystore.properties`) en dos lugares fuera de esta PC, por ejemplo en una memoria USB y en tu Google Drive o correo personal. Sin esa carpeta no se pueden publicar actualizaciones.

## Instalar la app en el celular del restaurante (primera vez)

1. Pasa el archivo `almuerzos-chelita-1.0.0.apk` al celular: por WhatsApp (como documento), por cable USB o por Google Drive.
2. En el celular, toca el archivo. Android preguntará si permite instalar apps de esa fuente (WhatsApp, Archivos, Drive...): elige **Permitir** y luego **Instalar**.
3. Si aparece una advertencia de Play Protect ("app desconocida"), toca **Más detalles → Instalar de todas formas**. Aparece porque la app no viene de Play Store.
4. Abre la app. Arriba aparece un aviso rojo: tócalo y en **Ajustes → Respaldos** elige la carpeta de respaldos. Recomendado: dentro de *Documentos*, crea una carpeta nueva llamada `Respaldos Chelita`. Sin este paso no se guardan respaldos.
5. En **Ajustes** revisa los precios y anota el menú del día.

### Si en ese celular ya hay una versión de prueba instalada

Las versiones de prueba anteriores a esta se firmaron con otra clave, así que Android no deja instalar la versión final encima. Si esa versión de prueba tiene datos que quieres conservar:

1. En la versión de prueba: **Ajustes → Respaldos → Elegir carpeta** y luego **Respaldar ahora**.
2. Desinstala la versión de prueba.
3. Instala la versión final (pasos de arriba).
4. Al elegir la misma carpeta de respaldos, la app ofrece restaurar el respaldo: toca **Restaurar**.

Las versiones de prueba nuevas se llaman **"Chelita PRUEBA"** y se instalan como otra app, así que ya no chocan con la versión final.

## Entregar una versión nueva

1. En `app/build.gradle.kts`, sube `versionCode` en 1 (por ejemplo, de `1` a `2`) y cambia `versionName` (por ejemplo, `"1.1.0"`).
2. En la terminal de Android Studio, dentro de la carpeta del proyecto:

   ```
   .\gradlew.bat assembleRelease
   ```

3. La APK queda en `app\build\outputs\apk\release\app-release.apk`. Cópiala a `Documents\AlmuerzosChelita\apk\` con el nombre de la versión, por ejemplo `almuerzos-chelita-1.1.0.apk`.
4. Antes de instalarla, toca **Respaldar ahora** en la app del celular, por precaución.
5. Pasa la APK al celular e instálala **encima** de la anterior, **sin desinstalar**. Android dirá "¿Quieres actualizar esta app?". Los pedidos y precios se conservan.

## Reglas que nunca hay que romper

- **No cambiar `applicationId`** (`com.chelita.almuerzos`). Si cambia, Android la trata como otra app.
- **No perder ni cambiar la clave de firma.**
- **No desinstalar para actualizar.** Siempre se instala encima.
- **Si cambian las tablas de la base de datos**, hay que subir `version` en `AlmuerzosDatabase` y escribir una migración. Nunca usar `fallbackToDestructiveMigration()`, porque borraría todos los pedidos.

## Probar en la PC

- **▶ Run** en Android Studio instala la versión de prueba "Chelita PRUEBA" en el emulador o en un celular conectado. No toca los datos de la versión final.
- Pruebas automáticas: `.\gradlew.bat testDebugUnitTest`.

# LabsCM20262-Gr04

Laboratorios del curso **Computación Móvil** — Universidad de Antioquia, semestre 2026-2, Grupo 04.

| Laboratorio | Carpeta | Descripción |
|-------------|---------|-------------|
| Lab 1 — Interfaz Gráfica de Usuario | [`Lab1-UI/`](Lab1-UI/) | App Android en Jetpack Compose para ingresar y mostrar los datos de un contacto. |

- **Repositorio:** https://github.com/Xion07/LabsCM20262-Gr04
- **Integrantes:** ver [`INTEGRANTES.md`](INTEGRANTES.md)

---

## Lab 1 — `Lab1-UI/`

### Requisitos
- Android Studio (Koala 2024.1.1 o superior).
- **JDK 17** para Gradle. Si Android Studio trae un JBR más nuevo (25 en AS 2026.1),
  Gradle 8.9 no arranca con él: en `Settings → Build Tools → Gradle → Gradle JDK`
  selecciona un JDK 17 (descarga uno con "Download JDK…" si no tienes).
- `minSdk 21` (Android 5.0) · `targetSdk`/`compileSdk 34`.
- Gradle 8.9 · AGP 8.6.1 · Kotlin 2.0.20 · Compose BOM 2024.09.02.
- Build verificado por línea de comandos: `assembleDebug` y `assembleRelease` OK.

### Estructura
```
Lab1-UI/
 └─ app/src/main/
     ├─ java/co/edu/udea/compumovil/gr04_20262/lab1/
     │   ├─ MainActivity.kt            menú con acceso a las dos pantallas
     │   ├─ PersonalDataActivity.kt    Nombres*, Apellidos*, Sexo, Fecha nac.*, Escolaridad
     │   └─ ContactDataActivity.kt     Teléfono*, Dirección, Email*, País*, Ciudad
     └─ res/
         ├─ values/strings.xml         idioma por defecto (español)
         ├─ values-en/strings.xml      inglés
         └─ mipmap-*/ic_launcher*      ícono personalizado (PNG para API<26 + adaptive para API 26+)
```

### Características implementadas
- Jetpack Compose + Material 3.
- Teclados acordes: texto con mayúscula inicial y sin autocorrección (nombres/apellidos/dirección),
  teléfono (`KeyboardType.Phone`), email (`KeyboardType.Email`).
- `imeAction = Next` entre campos y `Done` en el último campo de cada pantalla.
- `windowSoftInputMode="adjustResize"`: el teclado nunca tapa el campo en edición.
- Persistencia ante cambio de configuración con `rememberSaveable` (sobrevive rotación).
- `PersonalDataActivity` con layout distinto en portrait y landscape.
- `DatePicker` que no permite fechas futuras para la fecha de nacimiento.
- Multilenguaje español / inglés (`values/` y `values-en/`).
- Ícono personalizado de la app.
- Validación de obligatorios al presionar el botón; con los datos válidos se escriben en **Logcat**
  (tags `InformacionPersonal` / `InformacionContacto`).

### Compilar y ejecutar
```bash
cd Lab1-UI
./gradlew installDebug      # instala en el emulador/dispositivo conectado
```
O abrir la carpeta `Lab1-UI/` en Android Studio y pulsar **Run**.

### Generar el APK firmado (entrega)

**Opción A — Android Studio (recomendada):**
`Build > Generate Signed App Bundle / APK… > APK > Create new… ` (guardar el `.jks`),
elegir *build variant* `release`, marcar V1+V2, y generar.
El APK queda en `Lab1-UI/app/release/app-release.apk`.

**Opción B — línea de comandos:**
1. Crear el keystore una vez:
   ```bash
   keytool -genkeypair -v -keystore lab1-release.jks -alias lab1 \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Crear `Lab1-UI/keystore.properties` (NO se versiona):
   ```properties
   storeFile=../lab1-release.jks
   storePassword=TU_PASSWORD
   keyAlias=lab1
   keyPassword=TU_PASSWORD
   ```
3. Compilar:
   ```bash
   cd Lab1-UI
   ./gradlew assembleRelease
   ```
   APK firmado en `Lab1-UI/app/build/outputs/apk/release/app-release.apk`.

> El `build.gradle.kts` del módulo `app` detecta `keystore.properties` y firma el `release`
> automáticamente; si el archivo no existe, el `release` queda sin firmar.

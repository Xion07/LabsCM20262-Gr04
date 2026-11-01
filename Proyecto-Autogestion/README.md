# Motos del Caribe · Autogestión de crédito

App Android (Kotlin + Jetpack Compose) para que el arrendatario consulte su contrato, vea multas, solicite ampliación de plazo, envíe excusas con soporte y reciba alertas. Proyecto de Computación Móvil, Universidad de Antioquia.

## Estado

La app ya habla con la API (`../Proyecto-Autogestion-API`). Sin API (o con `-PuseMock=true`) usa datos simulados (`MockCreditRepository`).

| Módulo | Estado |
|---|---|
| Ingreso (cédula + clave) | Simulado: acepta cédula de 6+ dígitos y clave de 4+ caracteres. Cerrar sesión desde Inicio |
| Inicio, Saldo y abonos, Multas | Listo con datos mock |
| Plazo y excusas | Listo: fecha opcional, motivo (mín. 10 caracteres), foto de soporte y confirmación al enviar. No se envía a ningún servidor |
| Kilometraje | Listo: muestra los km recorridos desde el inicio del contrato. El aviso de cambio de aceite (`MaintenancePlan`, cada 3.000 km, aviso a 200 km) queda apagado hasta que exista el dato del último cambio |
| Alertas de pago | Listo con WorkManager y notificaciones locales; se acortan de 24 h a 6 h y 3 h al vencer y entrar en mora (`AlertPolicy`) |
| Idiomas y pantallas | Español e inglés según el idioma del sistema; vertical, horizontal y tablet (riel lateral desde 600 dp); modo claro y oscuro |
| Conexión a la API | Listo: login con JWT, contrato, abonos, multas, km y solicitudes de plazo con foto. El token vive solo en memoria. Errores de red, clave incorrecta, bloqueo y sesión vencida con mensajes propios |
| API con base de datos real y autenticación definitiva | Pendiente (la API usa datos simulados) |
| Push desde servidor (FCM) | Pendiente: requiere proyecto Firebase y `google-services.json` |

## Estructura

- `data/`: modelos y `CreditRepository` (interfaz + mock). Aquí se enchufa la API.
- `domain/`: reglas sin Android (`AlertPolicy`, `MaintenancePlan`), con pruebas en `src/test`.
- `notifications/`: canales, `AlertWorker` y programador.
- `ui/`: tema, formato y pantallas. `CreditViewModel` guarda el estado.

## Correr contra la API

1. Levantar la API (ver `../Proyecto-Autogestion-API/README.md`): `API_JWT_SECRET=<32+ caracteres> ./mvnw spring-boot:run`.
2. Compilar e instalar el debug en el emulador: la app apunta a `http://10.0.2.2:8080` (así el emulador ve el computador; el tráfico sin cifrar solo se permite hacia esa dirección y solo en debug).
3. Ingresar con la cédula `1000000001` y la clave `demo1234` (usuario de demostración de la API).

Opciones de Gradle: `-PuseMock=true` (datos simulados, sin API), `-PapiBaseUrl=https://...` (otra URL; en release es obligatoria y debe ser https; vacía = datos simulados).

Las alertas no usan el token: al cargar los datos la app guarda en el teléfono solo la próxima cuota (fecha, monto, km) y el trabajo de segundo plano lee esa copia; se borra al cerrar sesión.

## Compilar (Git Bash)

```
export JAVA_HOME="$HOME/Java/jdk-17.0.20.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew testDebugUnitTest assembleDebug
```

APK en `app/build/outputs/apk/debug/app-debug.apk`. Requiere `local.properties` con `sdk.dir` (no versionado).

## Datos reales (referencia)

Las vistas de `db_packgps` que alimentan el tablero de Power BI (`reporte_estado_motos`, `vw_gd_recaudo_bruto`, `vw_gd_motos_app_proyeccion`, `np_justi_super_org`) cubren saldo, abonos, plazo y justificaciones.

### Kilometraje (confirmado por el equipo de datos, 1-nov-2026)

- Fuente: tabla `hist_distancia`, vista `vw_sv_hist_distancia`. Una fila por dispositivo y día, del 2-nov-2022 al 31-oct-2026, ~4.500 dispositivos (2,1 M de filas en 2025-2026).
- Campos: `iddevice`, `fecha`, `distancia`, `movimiento`, `velocidadmax`, `hora_ini`, `hora_fin`, `alarma`, `sincronizo`, `placa`.
- Distancia diaria típica ~100 km (mediana 95). **Se asume que está en km; falta confirmarlo con quien administra el GPS.**
- Los km son de la **moto**, no del cliente. La columna `placa` viene vacía en ~7 de cada 10 filas desde 2025: **no usarla para unir**, ligar por `iddevice`.
- Ruta de unión: `iddevice` → placa con `dispositivos` (3.187 filas: `iddevice`, `imei`, `placa`; la vista `ubicacion` también trae contrato e `iddevice`) → contrato con `motos_app_hist` (`placa`, `contrato`, `fecha_inicio` diario desde 2024).
- Km del cliente = suma de `distancia` de la placa entre la fecha de inicio del episodio y su cierre.

### Decisión sobre el cambio de aceite (1-nov-2026)

No hay ningún dato en la BD que diga cuándo se hizo el último cambio de aceite. Decisión del equipo: la app **solo muestra los km recorridos** y se **propone a nivel de proyecto** agregar el registro del último cambio de aceite en el sistema.

- En el código, `Contract.lastOilChangeKm` es `null` mientras ese dato no exista. Con `null`, la pantalla Kilometraje muestra una tarjeta "Próximamente" y no se dispara ningún aviso de aceite.
- Cuando el dato exista, basta que el repositorio lo llene: `MaintenancePlan`, las notificaciones y la tarjeta de progreso ya funcionan con él.
- Ninguna de estas tablas está en el modelo de Power BI; la API tendrá que consultarlas directamente y sumar los km en la base, no en la app.

## Revisión con agentes (1-nov-2026)

Se revisó la app con cuatro agentes (seguridad, funcionalidad, experiencia de usuario e interfaz). Aplicado: textos en `strings.xml` (es/en), notificaciones que abren la pantalla correcta y no muestran montos en pantalla bloqueada, `allowBackup` apagado, release ofuscado (R8), tema Material 3 completo con la marca, barra de estado integrada, ícono propio, riel lateral en pantallas anchas, estados vacíos y de carga, y confirmación al enviar la solicitud.

Pendiente por decisión o por dependencia externa: base de datos real y autenticación definitiva en la API, HTTPS con certificate pinning, notificaciones push desde servidor (FCM), splash screen, vistas previas de Compose, ventana horaria de las alertas y persistir la sesión.

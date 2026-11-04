# API de autogestión · Motos del Caribe

API REST (Spring Boot 4.0.9, Java 17) para la app móvil de `Proyecto-Autogestion`.

**Es un proyecto de la universidad: no va a producción y no toca la base de la empresa.** Trae su propia base pequeña de **20 clientes FICTICIOS** (perfil `seed`, el predeterminado). Nombres, cédulas y placas son inventados; las fechas se calculan respecto a "hoy" para que la demo siempre muestre mora, vencimientos cercanos, clientes sin multas y un cliente nuevo sin abonos.

## Cómo correrla

```bash
export JAVA_HOME="$HOME/Java/jdk-17.0.20.1+1"
export API_JWT_SECRET="una-clave-de-minimo-32-caracteres"   # obligatoria; sin ella no arranca
./mvnw spring-boot:run          # http://localhost:8080
./mvnw test                     # 19 pruebas
```

Clientes de demostración (perfil `seed`): cédulas `1000000001` a `1000000020`, todas con la clave `demo1234`.

| Cédula | Situación |
|---|---|
| `1000000001` | Camilo Restrepo, al día (vence en 5 días) |
| `1000000002`, `06`, `07`, `12`, `18` | En mora |
| `1000000003` | Vence hoy |
| `1000000004`, `1000000015` | Vence mañana |
| `1000000005`, `10`, `15` | Sin multas |
| `1000000018` | Cliente nuevo, sin abonos |

Para regenerar los datos: `python tools/generate_seed.py` (es determinista). El formato de `seed/clients.json` admite reemplazarlo por datos reales **anonimizados** (sin nombres, cédulas ni placas verdaderas). El perfil `mock` (un solo usuario `1000000001`) lo usan las pruebas.

## Endpoints

Todo exige `Authorization: Bearer <token>` salvo las rutas `/api/v1/auth/*`. La cédula sale **siempre del token**, nunca de un parámetro.

| Método y ruta | Qué hace |
|---|---|
| `POST /api/v1/auth/authorize` | `{document, password, codeChallenge, codeChallengeMethod: "S256"}` → `{code, expiresInSeconds}`. Código de un solo uso, 60 s (PKCE, RFC 7636) |
| `POST /api/v1/auth/token` | `{code, codeVerifier}` → `{accessToken, refreshToken, tokenType, expiresInSeconds}`. Access token de 15 min, refresh token de 7 días. El código se consume al primer intento |
| `POST /api/v1/auth/refresh` | `{refreshToken}` → tokens nuevos. El refresh token rota: el anterior deja de servir |
| `POST /api/v1/auth/logout` | `{refreshToken}` → 204. Invalida el refresh token |
| `GET /api/v1/me/contract` | Saldo, próxima cuota, plazo restante, km recorridos. `lastOilChangeKm` es `null` (no existe en la BD) |
| `GET /api/v1/me/payments` | Historial de abonos |
| `GET /api/v1/me/fines` | Multas (`reason` es un código; la app lo traduce) |
| `GET /api/v1/me/km-history` | Historial de kilometraje |
| `GET /api/v1/me/extension-requests` | Historial de solicitudes del usuario, la más reciente primero (`status`: `PENDING_REVIEW`, `APPROVED` o `REJECTED`) |
| `POST /api/v1/me/extension-requests` | `{dueDate, newDate?, excuse}` → 201. Motivo de 10 a 500 caracteres; `newDate` posterior a `dueDate` |
| `POST /api/v1/me/extension-requests/{id}/attachment` | Multipart `file`: JPEG o PNG, máx. 5 MB |

Errores en JSON: `{"error": "..."}`; los de validación traen además `fields`.

## Seguridad implementada

- JWT HS256 de 15 min; clave por variable de entorno (mínimo 32 caracteres), **nunca en el repo**.
- Bloqueo de 15 min tras 5 intentos fallidos por cédula (en memoria: sirve para una sola instancia).
- Mismo mensaje para cédula inexistente y clave incorrecta, con tiempo de respuesta parecido.
- Cada recurso se resuelve por el dueño del token; una solicitud ajena responde 404.
- Adjuntos: el tipo se decide por los primeros bytes (no por el nombre), límite de 12.000 px por lado y se re-codifican (se pierde el EXIF, que puede traer GPS). Se guardan en una carpeta privada, sin URL pública.
- Respuestas de error sin trazas ni mensajes internos.

## Desplegada en Azure (demo)

**https://autogestion-api-jc.azurewebsites.net** (HTTPS obligatorio). Recursos nuevos y aislados, sin tocar ningún otro proyecto de la cuenta:

| Recurso | Nombre | Detalle |
|---|---|---|
| Grupo de recursos | `rg-autogestion-demo` | Se puede borrar completo para eliminar todo: `az group delete -n rg-autogestion-demo` |
| Plan de App Service | `plan-autogestion-f1` | F1 (gratis), Linux, España Central (la región North Central US ya usa su único plan gratis) |
| Web app | `autogestion-api-jc` | Java 17, TLS 1.2 mínimo, FTP desactivado, HTTP redirige a HTTPS |

Configuración (variables de la web app, **no están en el repo**): `API_JWT_SECRET` (generada al azar), `WEBSITES_PORT=8080`, `API_UPLOADS_DIR=/home/uploads`, `JAVA_OPTS=-Xmx400m`, `SPRING_PROFILES_ACTIVE=seed`.

Para volver a publicar: `./mvnw -q package -DskipTests` y `az webapp deploy -g rg-autogestion-demo -n autogestion-api-jc --src-path target/autogestion-api-0.0.1-SNAPSHOT.jar --type jar`.

## Despliegue gratis en Azure (plan F1)

El jar pesa ~25 MB. Se publica en un App Service Linux de plan **F1 (gratis)** con runtime Java 17 o 21 y estas variables: `API_JWT_SECRET` (32+ caracteres). Limitaciones del plan gratis: la API se duerme tras unos minutos sin uso y el primer pedido tarda hasta ~1 min (la app espera hasta 60 s), y tiene un tope diario de CPU. Las solicitudes de plazo y las fotos viven en memoria y en disco temporal: se pierden al reiniciar. Sin VPN, sin Relay y sin base de datos de pago.

## Si algún día fuera real

Lo que sigue **no aplica a la demo** de la universidad; queda como referencia.

1. **Base de datos real.** Reemplazar `MockCreditData` por una implementación que lea vistas de MySQL con un usuario de **solo lectura**. Tablas de referencia (las mismas del tablero de Power BI): `reporte_estado_motos` (saldo, cuota), `vw_gd_recaudo_bruto` (abonos), `vw_gd_motos_app_proyeccion` (plazo restante), `rentabilidad_activos` (multas) y, para el kilometraje, `vw_sv_hist_distancia` → `dispositivos` (por `iddevice`, no por `placa`) → `motos_app_hist`. Sumar los km en la base, no en la API. Mientras la BD solo admite vistas nuevas, las consultas deben ir en vistas propias.
2. **Autenticación real.** La BD no guarda credenciales de arrendatarios. Hay que decidir: clave propia en una tabla nueva, o código OTP por WhatsApp. Cuando se decida, se implementa `UserDirectory`.
3. **Guardar las solicitudes de plazo** en una tabla propia y avisar a cobranza (hoy viven en memoria).
4. **Refresh token** rotativo (hoy el token dura 15 min y no se renueva).
5. **HTTPS** en el despliegue (la API no lo termina por sí misma) y, en la app, `network_security_config` con certificate pinning.
6. **Datos personales** (Ley 1581 de 2012; datos crediticios, Ley 1266): aviso de privacidad, política de retención y auditoría de accesos.
7. La `X-API-Key` compartida de n8n y Azure **no debe usarse** con esta API ni ir dentro del APK.

# API de autogestión · Motos del Caribe

API REST (Spring Boot 4.0.9, Java 17) para la app móvil de `Proyecto-Autogestion`. **Hoy usa datos simulados** (perfil `mock`); falta conectar la base real.

## Cómo correrla

```bash
export JAVA_HOME="$HOME/Java/jdk-17.0.20.1+1"
export API_JWT_SECRET="una-clave-de-minimo-32-caracteres"   # obligatoria; sin ella no arranca
./mvnw spring-boot:run          # http://localhost:8080
./mvnw test                     # 12 pruebas
```

Usuario de demostración (solo perfil `mock`): cédula `1000000001`, clave `demo1234`.

## Endpoints

Todo exige `Authorization: Bearer <token>` salvo el login. La cédula sale **siempre del token**, nunca de un parámetro.

| Método y ruta | Qué hace |
|---|---|
| `POST /api/v1/auth/login` | `{document, password}` → `{accessToken, tokenType, expiresInSeconds}` (token de 15 min) |
| `GET /api/v1/me/contract` | Saldo, próxima cuota, plazo restante, km recorridos. `lastOilChangeKm` es `null` (no existe en la BD) |
| `GET /api/v1/me/payments` | Historial de abonos |
| `GET /api/v1/me/fines` | Multas (`reason` es un código; la app lo traduce) |
| `GET /api/v1/me/km-history` | Historial de kilometraje |
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

## Lo que falta para producción

1. **Base de datos real.** Reemplazar `MockCreditData` por una implementación que lea vistas de MySQL con un usuario de **solo lectura**. Tablas de referencia (las mismas del tablero de Power BI): `reporte_estado_motos` (saldo, cuota), `vw_gd_recaudo_bruto` (abonos), `vw_gd_motos_app_proyeccion` (plazo restante), `rentabilidad_activos` (multas) y, para el kilometraje, `vw_sv_hist_distancia` → `dispositivos` (por `iddevice`, no por `placa`) → `motos_app_hist`. Sumar los km en la base, no en la API. Mientras la BD solo admite vistas nuevas, las consultas deben ir en vistas propias.
2. **Autenticación real.** La BD no guarda credenciales de arrendatarios. Hay que decidir: clave propia en una tabla nueva, o código OTP por WhatsApp. Cuando se decida, se implementa `UserDirectory`.
3. **Guardar las solicitudes de plazo** en una tabla propia y avisar a cobranza (hoy viven en memoria).
4. **Refresh token** rotativo (hoy el token dura 15 min y no se renueva).
5. **HTTPS** en el despliegue (la API no lo termina por sí misma) y, en la app, `network_security_config` con certificate pinning.
6. **Datos personales** (Ley 1581 de 2012; datos crediticios, Ley 1266): aviso de privacidad, política de retención y auditoría de accesos.
7. La `X-API-Key` compartida de n8n y Azure **no debe usarse** con esta API ni ir dentro del APK.

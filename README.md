# Motos del Caribe · Autogestión de crédito

Aplicativo móvil para que el arrendatario de una moto de Motos del Caribe consulte y gestione su crédito sin tener que llamar a cobranza. Proyecto de aula de **Computación Móvil**, Universidad de Antioquia, 2026-2, Grupo 04 ([integrantes](INTEGRANTES.md)).

**Qué permite**
- Ver el saldo, los abonos y los kilómetros recorridos.
- Consultar el historial de multas y el tiempo restante del contrato.
- Pedir más plazo, con motivo y foto de soporte, y seguir el estado de cada solicitud.
- Recibir alertas de pago que se vuelven más frecuentes al acercarse el vencimiento (24 h → 6 h → 3 h).
- Ver el aviso de mantenimiento por kilometraje (pendiente de que exista el dato del cambio de aceite).

El objetivo del proyecto es reducir en 20 % las llamadas a cobranza por consultas de este tipo.

## Qué contiene el repositorio

| Carpeta | Qué es |
|---|---|
| [`Proyecto-Autogestion/`](Proyecto-Autogestion/) | App Android (Kotlin, Jetpack Compose, Material 3). Español e inglés, claro y oscuro, vertical y horizontal. |
| [`Proyecto-Autogestion-API/`](Proyecto-Autogestion-API/) | API REST (Spring Boot 4, Java 17, JWT) con 20 clientes ficticios. Desplegada en Azure. |
| [`docs/`](docs/) | Arquitectura, modelo entidad-relación y presentación final. |

## Documentación

- **Arquitectura (diagrama interactivo):** [verla en línea](https://htmlpreview.github.io/?https://github.com/Xion07/LabsCM20262-Gr04/blob/main/docs/arquitectura/index.html) · archivo en [`docs/arquitectura/`](docs/arquitectura/)
- **Modelo entidad-relación:** [`docs/modelo-entidad-relacion.md`](docs/modelo-entidad-relacion.md)
- **Presentación final (LaTeX/Beamer):** [`docs/presentacion/`](docs/presentacion/)
- **Detalle de la app:** [`Proyecto-Autogestion/README.md`](Proyecto-Autogestion/README.md)
- **Detalle de la API y su despliegue:** [`Proyecto-Autogestion-API/README.md`](Proyecto-Autogestion-API/README.md)

## Probar la app

La API de demostración está en **https://autogestion-api-jc.azurewebsites.net**. Cualquier compilación de la app apunta a ella por defecto, así que quien clone el repo ya puede usarla.

- **Usuarios de demostración:** cédulas `1000000001` a `1000000020`, todas con la clave `demo1234`. Son datos ficticios.
- **Primera consulta lenta:** el plan gratis de Azure duerme la API tras un rato sin uso, y la primera consulta puede tardar hasta un minuto. La app espera y muestra un indicador de carga.
- **Bloqueo:** cinco claves incorrectas seguidas bloquean esa cédula durante 15 minutos.

Para compilar (Git Bash, con JDK 17 y el SDK de Android):

```bash
cd Proyecto-Autogestion
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

Para instalar el APK en un celular Android hay que permitir "instalar apps desconocidas". Las opciones para correr contra una API local o con datos simulados, y el procedimiento del APK firmado, están en el [README de la app](Proyecto-Autogestion/README.md).

## Estado

Funcional de punta a punta contra la API en Azure con datos de demostración. Queda pendiente, por depender de decisiones o de otros sistemas: conectar la base de datos real de la empresa (con vistas de solo lectura), la autenticación definitiva, las notificaciones push desde servidor y el registro del último cambio de aceite. Detalle en la sección "Si algún día fuera real" del [README de la API](Proyecto-Autogestion-API/README.md).

---

*El Lab 1 del curso (interfaz gráfica de contacto) vive en [`Lab1-UI/`](Lab1-UI/).*

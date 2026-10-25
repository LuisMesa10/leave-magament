# Leave Management API

API REST educativa para registrar y decidir solicitudes de permisos. Java 17, Spring Boot 3.5, Spring Data JPA, Bean Validation y H2.

## Arquitectura

Arquitectura en capas: controlador HTTP (Spring MVC) -> servicio transaccional -> repositorio JPA -> H2.
Spring MVC se usa en modo REST: las respuestas son JSON, no vistas HTML.
El DTO de entrada separa los datos del cliente de la entidad persistente.

## Ejecutar

Requisitos: JDK 17 o superior y Maven 3.9+.

```shell
mvn spring-boot:run
mvn test
```

Alternativa con wrapper del repositorio: `./mvnw test` (Linux/macOS) o `.\mvnw.cmd test` (Windows).
La primera ejecucion necesita Internet para descargar Maven y dependencias.
API local: http://localhost:8080/api/leaves

## Endpoints

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| POST | /api/leaves | Crea solicitud pendiente; 201 y Location |
| GET | /api/leaves | Lista solicitudes |
| PUT | /api/leaves/{id}/approve | Aprueba solicitud pendiente |
| PUT | /api/leaves/{id}/reject | Rechaza solicitud pendiente |
| DELETE | /api/leaves/{id} | Elimina; 204 |
| GET | /api/leaves/export | Descarga texto UTF-8 |

Ejemplo de cuerpo POST:

```json
{"employeeName":"Ana Perez","type":"VACATION"}
```

Nombre y tipo son obligatorios (maximos 120 y 80 caracteres).
El tipo es texto libre; no existe un catalogo cerrado de permisos.
El servidor asigna ID, fecha y estado PENDING. Se rechazan campos desconocidos.
Solo se admite PENDING -> APPROVED o PENDING -> REJECTED; las decisiones posteriores devuelven 409.
Un identificador inexistente devuelve 404; los datos invalidos devuelven 400.
Los errores HTTP usan Problem Details. El bloqueo optimista evita sobrescribir decisiones concurrentes.
La exportacion devuelve el contenido al cliente sin escribir archivos compartidos en disco.

## Pruebas y CI

Pruebas de integracion con MockMvc, contexto Spring y H2 real: validacion,
proteccion del ID, creacion, transiciones, 404, eliminacion y descarga.
GitHub Actions ejecuta `mvn verify` con Java 17.

## Alcance y limitaciones

Proyecto de portafolio en desarrollo. H2 esta en memoria: los datos se pierden al reiniciar.
La consola H2 esta deshabilitada. No incluye autenticacion, autorizacion, integracion con
Directorio Activo, paginacion ni interfaz web. No debe exponerse con datos reales.
No implementa microservicios. Docker es una posible mejora futura, no una funcion existente.

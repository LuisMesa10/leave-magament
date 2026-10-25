# Leave Management API

API REST para gestionar solicitudes de permisos: empleado, supervisor,
estados, fechas, consulta paginada y trazabilidad. Java 17 y Spring Boot 3.5.

## Funcionalidades

- Arquitectura en capas y Spring MVC para una API REST, con DTOs de entrada y salida.
- Spring Security: autenticacion HTTP Basic, BCrypt y autorizacion por rol y propietario.
- Spring Data JPA, relaciones entre entidades, transacciones y bloqueo optimista.
- Validacion de datos, respuestas Problem Details y pruebas de integracion con MockMvc/H2.
- Filtros, paginacion, resumen de estados y exportacion CSV.
- CI con Java 17 y empaquetado Docker.

No hay vistas HTML ni interfaz web: Spring MVC trabaja aqui con controladores REST.

## Inicio rapido

Requisitos: JDK 17+ y Maven 3.9+. Las claves deben ser distintas, de al menos
12 caracteres y definidas fuera del repositorio. Las cuentas de demostracion son
`employee` y `manager`. El arranque falla si las claves no se configuran.

PowerShell (sustituya los valores de ejemplo por claves propias):

```powershell
$env:LEAVE_EMPLOYEE_PASSWORD = "una-clave-local-empleado"
$env:LEAVE_MANAGER_PASSWORD = "otra-clave-local-supervisor"
mvn spring-boot:run
```

Sin perfil se utiliza H2 en memoria, que pierde los datos al reiniciar.
Para conservar datos de demostracion local:

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

En Windows, si PowerShell interpreta el argumento, use
`mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
El perfil local guarda H2 en `data/`, excluido de Git.
La consola H2 permanece deshabilitada.

## Flujo de uso

1. El empleado crea una solicitud: ID, propietario, estado y fecha los asigna el servidor.
2. Consulta sus solicitudes, filtra por tipo/estado/periodo y revisa el detalle.
3. Puede actualizar una solicitud pendiente enviando su version actual, o cancelarla con motivo.
4. El supervisor consulta todas las solicitudes y aprueba o rechaza con comentario obligatorio.
5. Ambos consultan el historial segun sus permisos y descargan CSV con los mismos filtros.
6. Los estados finales no se pueden editar ni revertir. El historial permanece.

Para escrituras se requiere el encabezado `X-Requested-With: leave-api`.
Se utiliza HTTP Basic sin sesiones, exclusivamente para clientes API.
El encabezado obligatorio y la ausencia de CORS evitan escrituras desde formularios
de otros origenes con credenciales Basic recordadas. No se ofrece login de navegador.
Fuera de localhost, este esquema necesita HTTPS.

Ejemplo con curl (pide la clave sin escribirla en el comando).
Las fechas deben ser iguales o posteriores a la fecha del servidor:

```shell
curl -u employee -H "Content-Type: application/json" -H "X-Requested-With: leave-api" -d '{"employeeName":"Ana Perez","type":"VACATION","startDate":"2030-06-10","endDate":"2030-06-12","reason":"Descanso"}' http://localhost:8080/api/leaves
curl -u employee "http://localhost:8080/api/leaves?status=PENDING&page=0&size=10"
curl -u manager -H "Content-Type: application/json" -H "X-Requested-With: leave-api" -d '{"comment":"Cobertura verificada"}' http://localhost:8080/api/leaves/1/approve
```

En PowerShell use `curl.exe` y un archivo JSON con `--data-binary @solicitud.json`
si su version de la consola modifica las comillas de argumentos.

## Documentacion

- [Contrato HTTP, ejemplos y errores](docs/api.md)
- [Arquitectura, reglas y decisiones](docs/architecture.md)

## Pruebas

```shell
mvn verify
```

Tambien se puede usar `./mvnw verify` o `.\\mvnw.cmd verify`.
La primera ejecucion requiere acceso a Maven Central.
Las pruebas usan H2 en memoria y claves sinteticas exclusivas de `src/test/resources`;
no necesitan variables de entorno ni modifican los datos del perfil local.

Cubren autenticacion, permisos, aislamiento de solicitudes, validacion de entradas,
edicion con control de version, decisiones, cancelacion, historial, filtros, paginacion,
resumen y CSV. GitHub Actions ejecuta la verificacion completa en cada push y PR.

## Docker

```shell
docker compose up --build
```

Configure primero `LEAVE_EMPLOYEE_PASSWORD` y `LEAVE_MANAGER_PASSWORD` en el entorno.
Compose exige ambas variables, limita la exposicion a localhost y conserva H2
en un volumen. `docker compose down` conserva ese volumen.
El contenedor ejecuta la aplicacion con un usuario sin privilegios.


El perfil local usa actualizacion automatica de esquema solo para demostraciones:
no migra datos de versiones anteriores. Para produccion harian falta una base administrada,
migraciones versionadas, gestion corporativa de usuarios, operacion y revision de seguridad.

# Contrato HTTP

Base local: http://localhost:8080/api/leaves

Todos los endpoints requieren HTTP Basic. Cada escritura necesita
`X-Requested-With: leave-api` y los cuerpos usan `Content-Type: application/json`.
No se aceptan campos desconocidos.

| Metodo y ruta | Empleado | Supervisor |
| --- | --- | --- |
| POST / | Crear propia | 403 |
| GET / | Listar propias | Listar todas |
| GET /{id} | Propia | Cualquiera |
| PUT /{id} | Editar propia pendiente | 403 |
| POST /{id}/cancel | Cancelar propia pendiente | 403 |
| POST /{id}/approve | 403 | Aprobar pendiente |
| POST /{id}/reject | 403 | Rechazar pendiente |
| GET /{id}/history | Historial propio | Cualquier historial |
| GET /summary | Resumen propio | Resumen global |
| GET /export | CSV propio | CSV global |

Una solicitud ajena devuelve 404 al empleado para no revelar su existencia.
No hay borrado definitivo.

## Crear: POST /

```json
{
  "employeeName": "Ana Perez",
  "type": "VACATION",
  "startDate": "2030-06-10",
  "endDate": "2030-06-12",
  "reason": "Descanso"
}
```

Nombre: 1-120 caracteres no blancos. Motivo: 1-1000 caracteres no blancos.
Tipos: VACATION, SICK_LEAVE, PERSONAL, OTHER.
Fechas ISO, obligatorias; fin >= inicio y comienzo >= fecha actual del servidor.
No se admiten permisos retroactivos en esta demostracion.
Responde 201 con Location y detalle. ID, propietario, estado y fecha son del servidor.
El detalle incluye `version`, `calendarDays`, `requestDate` y `updatedAt`.

## Actualizar: PUT /{id}

```json
{
  "version": 0,
  "details": {
    "employeeName": "Ana Perez",
    "type": "PERSONAL",
    "startDate": "2030-06-10",
    "endDate": "2030-06-11",
    "reason": "Tramite personal"
  }
}
```

Se aplican las reglas de creacion. Solo el propietario y mientras este PENDING.
La version debe coincidir con la ultima lectura; una edicion desactualizada devuelve 409.
No es un PATCH: se deben enviar todos los campos de details.

## Aprobar, rechazar o cancelar

POST /{id}/approve, /{id}/reject o /{id}/cancel:

```json
{"comment":"Cobertura verificada"}
```

Comentario obligatorio, no blanco, maximo 1000 caracteres.
Estados: PENDING -> APPROVED, REJECTED o CANCELLED. Son estados finales.
Solo supervisor para approve/reject; solo propietario para cancel.
Responde 200 con el detalle actualizado.

## Buscar y exportar

GET / acepta:
- `status`: PENDING, APPROVED, REJECTED, CANCELLED.
- `type`: un valor del catalogo.
- `employee`: nombre exacto de usuario, hasta 100 caracteres.
- `from`, `to`: periodo ISO opcional; filtra por interseccion inclusiva con las fechas del permiso.
- `page`: base cero, por defecto 0.
- `size`: entre 1 y 100, por defecto 20.

Orden fijo: ID descendente.
`employee` nunca amplia el alcance del empleado: se intersecta con su identidad.

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
```

GET /{id}/history admite page y size, mismos limites; orden cronologico por ID.
Cada evento contiene ID, accion, actor, comentario y fecha. CREATED y UPDATED
incluyen instantanea textual de los datos; las decisiones incluyen el comentario del actor.

GET /summary devuelve conteos para los cuatro estados dentro del alcance del usuario.
No recibe filtros.

GET /export admite los mismos filtros que buscar, salvo page/size.
Entrega CSV UTF-8, nombres de columnas en la primera fila, celdas escapadas y
proteccion de formulas en campos de usuario. Orden ID ascendente.
Si hay mas de 1000 coincidencias, devuelve 400 y exige reducir el filtro;
no recorta silenciosamente el resultado.

## Errores

| Estado | Causa |
| --- | --- |
| 400 | JSON/enum/fecha invalida, validacion, rango, paginacion o exportacion excesiva |
| 401 | Faltan credenciales o son incorrectas |
| 403 | Rol no permitido, accion ajena o falta encabezado de escritura |
| 404 | No existe la solicitud dentro del alcance del usuario |
| 409 | Estado final, version desactualizada o escritura concurrente |

Errores en formato application/problem+json. Las validaciones de campos incorporan
un objeto `errors`. No se exponen stack traces ni detalles de la base.

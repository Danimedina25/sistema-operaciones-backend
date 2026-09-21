# Cheques recibidos: implementación y operación

## Modelo y contrato

El pago `operation_payments` es también el registro único del cheque: `Cheque.id = Cheque.pagoId = OperationPayment.id`. Se evita una segunda identidad o importe independiente. Estado y versión del cheque son separados de `PaymentStatus`. `chequeVersion` se compara bajo bloqueo pesimista y aumenta con cada edición o transición; no es una versión enviada libremente por el cliente.

Captura y edición añaden `numeroCheque`, `bancoEmisor`, `emisor`, `beneficiario` (obligatorios, hasta 200 caracteres) y aceptan `cuentaDestinoId: null` para CHEQUE. Una cuenta recibida en captura de cheque es rechazada. No se aceptan campos ajenos al DTO que permitan asignar estado o cobro. El comprobante debe usar HTTPS Firebase Storage y la ruta `comprobantes/operaciones/{operacionId}/...`; la autorización real de objetos sigue siendo responsabilidad de las reglas de Storage, no se descarga ni se hace una petición a la URL desde el backend.

Mientras POR_COBRAR se pueden corregir datos e importe, conservando historial. Cambiar un cheque a otro tipo requiere CANCELAR y registrar un pago sustituto; no se borra su historia. Un pago pendiente de otro tipo sí puede corregirse a CHEQUE. Después de depósito, cobro o conciliación pendiente no se permite editar ordinariamente.

El sistema actual trabaja con importes sin una columna de divisa; la API declara MXN. No se habilita captura multidivisa: introducirla requiere un contrato y contabilidad adicionales.

## Endpoints

Todos conservan `{success, message, data, errors, timestamp}` de `ApiResponse`.

- GET `/api/operations/cheques`: estados CSV (ausente = POR_COBRAR,DEPOSITADO; vacío = todos), busqueda, cliente, operacionId opcional, banco emisor, desde/hasta inclusivos sobre recepción, page base cero. Tamaño 20 y orden fecha/id descendente. Devuelve content, totalElements, totalPages, totales. Totales ignoran exclusivamente estados y paginación. Históricos sin estado aparecen al consultar todos.
- GET `/api/operations/cheques/by-payment/{paymentId}`: detalle con estado, versión, datos bancarios históricos e historial cronológico.
- POST `/api/operations/cheques/{id}/actions`: requestId UUID, version, accion, fecha ISO YYYY-MM-DD, campos condicionales de `ChequeCommand`. Devuelve el mismo `ChequeView` del frontend, incluyendo el evento recién registrado.

Lectura: ADMIN, JEFA_CUENTAS, AUXILIAR_CUENTAS, JEFA_CAJAS, GERENTE, DIRECCION. Estos roles ya tienen acceso de personal a operaciones; no se crea acceso del socio al listado. El socio obtiene el estado desde su operación.

| Acción | Desde | Hasta | Roles de escritura | Requisitos |
|---|---|---|---|---|
| DEPOSITAR | POR_COBRAR | DEPOSITADO | ADMIN, JEFA_CUENTAS, AUXILIAR_CUENTAS | cuenta activa y comprobante |
| COBRAR_BANCO | POR_COBRAR / DEPOSITADO | COBRADO | mismos | cuenta activa, comprobante, periodo abierto |
| COBRAR_EFECTIVO | POR_COBRAR | COBRADO | ADMIN, JEFA_CAJAS | caja de hoy abierta, fecha coincidente, 11 denominaciones exactas, comprobante |
| DEVOLVER | POR_COBRAR / DEPOSITADO | DEVUELTO | ADMIN, JEFA_CUENTAS, AUXILIAR_CUENTAS | motivo |
| CANCELAR | POR_COBRAR | CANCELADO | mismos | motivo |

No hay reapertura de terminales en la API. Devolución posterior a cobro requiere una reversión financiera auditada, fuera de estas acciones. Tampoco se permite eliminar una operación con cheques, ni borrar un día de caja con COBRO_CHEQUE_CLIENTE.

## Contabilidad y concurrencia

Recibir y depositar no validan el pago ni suman dinero disponible. Cobrar valida y recalcula operación/comisiones en la misma transacción. DEVUELTO/CANCELADO marca el pago RECHAZADA: libera la reserva para sustitución pero no disminuye la deuda efectivamente cobrada. Retornos siguen usando el ingreso validado existente.

El banco mantiene una sola fuente derivada del pago; no se inserta un segundo movimiento paralelo. Para cheques nuevos se exige COBRADO + CUENTA_BANCARIA y se usa `cheque_fecha_cobro`. Los históricos de estado NULL conservan exactamente la consulta previa. Caja usa COBRO_CHEQUE_CLIENTE, con operation_payment_id único y sin cuenta bancaria; CHEQUE manual conserva el significado de retiro de una cuenta propia.

Fecha de cobro es efectiva (inicio del día indicado porque el frontend captura fecha sin hora). Fecha de validación/registro conserva la hora real de auditoría. Consultas de entradas y cortes usan fecha efectiva para cheques nuevos y la previa para históricos. Los cheques de clientes cobrados en caja no se suman a entradas bancarias del corte global.

Orden de bloqueo: operación → pago → mutex de cierre/caja (cash_general_register) → cuenta/corte. Se refrescan entidades bajo bloqueo para no usar el snapshot anterior de MySQL REPEATABLE READ. Registro/reconstrucción de cortes usa el mismo mutex. Se rechaza cobro si existe un corte global cerrado o un corte de la cuenta en la fecha o posterior, para no invalidar saldos posteriores ya persistidos. Depósitos pueden registrarse porque no son movimientos financieros.

`cheque_audit.request_id` es único globalmente. Se guarda SHA-256 del comando canónico y la respuesta JSON original. Un replay autorizado se resuelve mediante lectura de bloqueo antes de comprobar versión/estado: devuelve el resultado original. Misma clave con otro cheque/cuerpo produce conflicto. Claves distintas con versión obsoleta producen conflicto sin efectos. Estado, pago, movimiento de caja, recálculo, comisiones e historial se confirman juntos. Las notificaciones se disparan después del commit; errores de entrega se registran en logs sin deshacer ni aparentar fallar el cobro confirmado.

## Despliegue y migración

1. Respaldar y revisar el esquema real antes de aplicar `migrations/2026-09-20_cheques_recibidos.sql` una sola vez. Requiere las migraciones anteriores, especialmente `operation_payment_id` único en caja. El script no cambia importes, estatus, cuentas ni fechas de pagos existentes.
2. Aplicar primero la migración y luego el backend. No basarse únicamente en ddl-auto=update: las restricciones adicionales del script también son necesarias.
3. Coordinar el despliegue con el frontend ya preparado. No se requieren cambios de nombres de sus endpoints o DTO.
4. Validar totales antes/después con el inventario y los libros de banco y caja. No aplicar un backfill automático a COBRADO.

### Conciliación de históricos

Todos los cheques preexistentes quedan `cheque_estado=NULL`; API responde estado null, requiereConciliacion true. Su contabilización previa se preserva y las acciones se bloquean. Validado o tener cuenta no prueba un cobro.

Para cada histórico, conciliar cheque, comprobante y extracto contra el pago y los movimientos existentes. Documentar estado real, destino, fecha, responsable y evidencia. Si ya existe una entrada válida, enlazarla/conservarla en la misma identidad de pago, sin invocar COBRAR ni crear otra entrada. Para un cobro bancario histórico confirmado, cualquier regularización de fecha debe demostrar que no cambia los cortes anteriores; de lo contrario requiere ajuste/reversión supervisado. Para efectivo, verificar `operation_payment_id` y no crear una segunda entrada.

La corrección administrativa debe ejecutarse en una transacción que bloquee operación/pago/mutex y deje auditoría, con comparación de saldos antes/después. No se entrega un script de actualización masiva ni un endpoint para convertir históricos sin evidencia. Los desconocidos permanecen bloqueados. No eliminar ni alterar el registro original para corregir diferencias: requieren un procedimiento financiero aprobado de reversión.

## Pruebas reproducibles

`JAVA_HOME=<JDK17> mvn test` utiliza exclusivamente `src/test/resources/application.properties` y H2 en modo MySQL; no arranca con el perfil dev.

Pruebas reales de MySQL, únicamente contra una instancia desechable de loopback:

```sh
JAVA_HOME=<JDK17> mvn -Dtest=ChequeIntegrationTest,ChequeContractTest \
  '-Dcheques.test.mysql-url=jdbc:mysql://127.0.0.1:33379/sdo_cheques_test?allowPublicKeyRetrieval=true&useSSL=false' test
```

El test rechaza URLs distintas de loopback/esquema sdo_cheques_test. Usa create-drop: nunca apuntarlo a una base existente de trabajo. El usuario root sin contraseña solo corresponde a la instancia temporal inicializada para pruebas, no a configuración de despliegue.

La suite cubre captura y edición, efectos por destino, reintentos y concurrencia, rollback ante error de caja o comisiones, permisos, históricos, reserva para sustitución, periodos cerrados, denominaciones, pruebas JSON y persistencia del libro. La suite previa conserva verificaciones de retornos y comisiones.

### Resultado verificado en esta implementación

- Java 17: compilación correcta.
- Suite completa en H2/MySQL mode: 192 pruebas, 0 fallos, 0 errores, 0 omitidas.
- MySQL 26.7 temporal: 28 pruebas de integración + 4 de contrato, sin fallos. Incluyen cobros parciales simultáneos en la misma operación, solicitudes de distintas operaciones y cierre de caja concurrente.
- Migración ejecutada en un segundo esquema temporal con pagos históricos representativos: columnas/restricciones creadas, estado desconocido preservado, importes y estatus anteriores intactos (`MIGRATION_OK`).
- No se ejecutaron migraciones ni se desplegó en bases de trabajo o producción.

# Recargas simuladas de MetroGo

El pago es una simulación. AWS guarda el comprobante y actualiza el saldo simulado de la tarjeta. No hay conexión con Yape, Plin, bancos ni con el saldo real del Metropolitano.

Android ya está preparado para estas rutas en tu API existente. Falta desplegar esta Lambda y asociar las rutas en tu consola AWS. No necesitas crear otra API ni otra tabla.

## 1. Crear la Lambda

En la misma cuenta y región de las tablas (`us-east-1`):

1. Lambda → Crear función → Crear desde cero.
2. Nombre: `metrogo-recargas`.
3. Runtime: Python 3.12. Utiliza el rol de ejecución permitido en tu laboratorio.
4. Copia el contenido completo de `lambda_function.py` en el archivo del mismo nombre. También puedes cargar `metrogo-lambda-recargas.zip` como archivo ZIP.
5. Controlador: `lambda_function.lambda_handler`.
6. Configura 256 MB de memoria y 15 segundos de tiempo de espera.
7. Pulsa **Deploy**.

## 2. Variables de entorno

Configuración → Variables de entorno → Editar. Cada fila lleva una clave y un valor, sin comillas:

| Clave | Valor |
|---|---|
| RECHARGES_TABLE_NAME | recargas_metrogo |
| SESSIONS_TABLE_NAME | sesiones_metrogo |
| USERS_TABLE_NAME | usuario_metrogo |
| CARDS_TABLE_NAME | tarjetas_metrogo |
| USER_CARDS_TABLE_NAME | usuario_tarjeta |

La tabla `recargas_metrogo` debe tener partición `usuarioId` (String) y ordenación `id` (String), como la que ya creaste. Las otras tablas conservan sus claves actuales.

## 3. Permisos del rol

El rol de ejecución necesita los permisos indicados en `permisos-iam.json`. Reemplaza `TU_ID_DE_CUENTA` por tu ID real. Si el rol del laboratorio ya permite estas operaciones, no necesitas agregar otra política.

| Tabla | Acciones |
|---|---|
| sesiones_metrogo y usuario_metrogo | GetItem |
| usuario_tarjeta | GetItem, ConditionCheckItem |
| tarjetas_metrogo | GetItem, UpdateItem |
| recargas_metrogo | GetItem, PutItem, Query |

Conserva los permisos habituales de CloudWatch Logs. La transacción utiliza los permisos de sus operaciones individuales.

## 4. Probar la Lambda

Los eventos individuales están en `eventos/` y se pueden copiar completos en Lambda → Test.

1. Inicia sesión con una cuenta registrada usando la Lambda de login y copia su `data.accessToken` reciente.
2. Sustituye `PEGA_AQUI_UN_ACCESS_TOKEN_RECIENTE` en cada evento. Conserva `Bearer ` antes del token.
3. En el evento de registrar, sustituye `8754785478` por una tarjeta activa vinculada a esa misma cuenta. Debe tener `saldoSimulado: true`.
4. Ejecuta `01_listar`. Resultado: 200 y tu historial, vacío si nunca recargaste.
5. Ejecuta `02_registrar`. Resultado: 201 y un comprobante con monto de S/ 20.00 (`montoCentimos: 2000`). La tabla de recargas tendrá un nuevo registro y el saldo de esa tarjeta aumentará en 2000 céntimos.
6. **Repite exactamente el mismo evento**: resultado 200, mismo comprobante y ningún incremento adicional de saldo.
7. Ejecuta `03_consultar` y vuelve a listar: la recarga debe aparecer.

Para otra recarga intencional usa un UUID nuevo en `idOperacion`. Para reintentar una operación incierta conserva su UUID, tarjeta, monto y método. No cambies el monto usando el mismo UUID: se rechaza con 409.

No envíes `usuarioId`, saldo, fecha ni estado en el POST: los determina el servidor. El monto se expresa en céntimos enteros, de 1 a 50000. Métodos válidos: `Yape`, `Plin`, `Tarjeta Débito / Crédito Visa`.

## 5. Asociar rutas en tu API existente

API Gateway → `apiMetrogo (yxonw4vey5)` → Rutas → Crear:

| Método | Ruta | Integración |
|---|---|---|
| POST | /recargas | metrogo-recargas |
| GET | /recargas | metrogo-recargas |
| GET | /recargas/{id} | metrogo-recargas |

Para cada ruta:

1. Asocia la integración a la Lambda `metrogo-recargas` en `us-east-1`.
2. Usa versión de carga **2.0**.
3. Autorización de API Gateway: **NONE**, igual que en tarjetas. La Lambda valida el token Bearer contra `sesiones_metrogo`.
4. Permite a API Gateway invocar la función; la consola suele configurar este permiso al agregar la integración.
5. Despliega en la **misma etapa** que ya usa el registro/login. Si tienes implementación automática, verifica que esté activada. No crees una etapa con prefijo adicional sin actualizar la URL Android.

La aplicación usa esta base:

`https://yxonw4vey5.execute-api.us-east-1.amazonaws.com/`

Por tanto las rutas finales serán `/recargas` y `/recargas/{id}` sobre esa base. Abrirlas directamente en el navegador sin token devuelve 401: la prueba completa se realiza desde la app, después de iniciar sesión.

## 6. Probar desde Android

1. Inicia sesión y vincula una tarjeta, si aún no la tienes.
2. Abre Recargar, elige la tarjeta, el monto y el método.
3. Confirma la simulación. El comprobante aparece solo después de la respuesta de AWS.
4. Ve a Inicio o Mis tarjetas: el saldo simulado se consulta de nuevo.
5. Abre Historial: debe aparecer la recarga. Cierra sesión y entra de nuevo para comprobar persistencia.
6. Entra con otro usuario: no debe ver el historial del primero.

Si hay un fallo de conexión, la aplicación conserva el intento y ofrece reintentar el mismo código. No genera un segundo pago ni un segundo registro para ese intento. La recuperación usa SavedStateHandle; no equivale a una cola persistente frente a borrado de datos o cierre forzado. El historial en AWS permite consultar los intentos ya confirmados.

Esta etapa guarda recargas y consulta el historial. El envío de reclamos a AWS sigue pendiente.

## Notas técnicas y pruebas

Registro y saldo se escriben con `TransactWriteItems`: ambos se confirman o ninguno cambia. Un registro condicionado por `usuarioId + idOperacion` evita duplicados, incluso después de la ventana de idempotencia del SDK. Se comprueba el vínculo activo dentro de la misma transacción y se protege el saldo contra actualizaciones simultáneas.

El historial consulta por `usuarioId`, nunca mediante un Scan general. El cursor está limitado al mismo usuario. Android recorre las páginas y ordena por fecha del servidor.

Referencia oficial: [transacciones de DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_TransactWriteItems.html).

Pruebas locales: instalar `boto3`, `pytest` y `moto[dynamodb]`; ejecutar `pytest test_lambda_function.py`. Moto intercepta AWS: las pruebas no escriben en tu cuenta.

# Lambda `metrogo-tarjetas` (HU-004 y HU-006)

Backend de **HU-004** (saldo, estado de la tarjeta y alerta de saldo bajo) y **HU-006** (puntos de recarga del mapa).

- **Código:** `lambda_function.py` (Python 3.12).
- **Pruebas:** `test_lambda_function.py` (DynamoDB simulada con moto).

## Recursos en AWS

| Recurso | Nombre | Configuración |
|---|---|---|
| Tabla DynamoDB | `Tarjetas` | Clave de partición `numeroTarjeta` (String) |
| Tabla DynamoDB | `PuntosRecarga` | Clave de partición `id` (String) |
| Lambda | `metrogo-tarjetas` | Python 3.12, rol `LabRole`, timeout 10 s |
| API Gateway (HTTP API) | `metrogo-tarjetas-api` | Las 4 rutas de abajo, integradas con la Lambda |

### Rutas

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/tarjetas/{numeroTarjeta}` | Devuelve saldo, estado y alertas. La primera vez registra la tarjeta con S/ 17.30. Acepta `?tipo=General`. |
| PUT | `/tarjetas/{numeroTarjeta}/alertas` | Body `{"umbralSaldoBajoCentimos": 500, "alertasActivas": true}` (umbral: 500, 1000, 1500 o 2000) |
| POST | `/tarjetas/{numeroTarjeta}/movimientos` | Body `{"tipo": "RECARGA", "monto": "20.00"}` o `{"tipo": "VIAJE"}` (descuenta S/ 1.60 estudiante o S/ 3.20 general) |
| GET | `/puntos-recarga` | Lista de estaciones. La primera vez carga las 13 estaciones en la tabla. |

Todas responden con el mismo formato que las Lambdas de login y registro:
```json
{"success": true, "message": "Saldo consultado", "data": { ... }}
```

## Despliegue en AWS Academy (Learner Lab)

1. **Inicia el laboratorio.** Presiona **Start Lab**, espera el punto verde y abre la consola con **AWS**. La región es **us-east-1** (N. Virginia).
2. **Crea las tablas en DynamoDB.** Ve a **DynamoDB > Tables > Create table**:
   - `Tarjetas`, con clave de partición `numeroTarjeta` de tipo **String**. Deja lo demás por defecto.
   - `PuntosRecarga`, con clave de partición `id` de tipo **String**.
3. **Crea la Lambda.** Ve a **Lambda > Create function > Author from scratch**:
   - Nombre: `metrogo-tarjetas`. Runtime: **Python 3.12**.
   - En *Change default execution role*, elige **Use an existing role** y luego **LabRole**.
   - Presiona **Create function**, pega el contenido de `lambda_function.py` en el editor y presiona **Deploy**.
   - En **Configuration > General configuration > Edit**, pon **Timeout = 10 s**.
4. **Crea el API.** Ve a **API Gateway > Create API > HTTP API > Build**:
   - En *Integrations*, agrega **Lambda** y elige `metrogo-tarjetas`. El nombre del API es `metrogo-tarjetas-api`.
   - Configura las 4 rutas de la tabla de arriba, todas con la misma integración.
   - Deja el stage **$default** con *Auto-deploy* activado y presiona **Create**.
   - Copia el **Invoke URL**, por ejemplo `https://abc123.execute-api.us-east-1.amazonaws.com`.
5. **Prueba en el navegador:**
   - `<Invoke URL>/puntos-recarga` debe mostrar las 13 estaciones.
   - `<Invoke URL>/tarjetas/8754785478` debe mostrar el saldo `17.30`.
6. **Conecta la app.** En `app/src/main/java/com/karenarias/proyectopmac/data/card/TarjetasApi.kt`, pega la URL en:
   ```kotlin
   const val BASE_URL = "https://abc123.execute-api.us-east-1.amazonaws.com/"
   ```

**Notas:**
- El API solo responde mientras el laboratorio está encendido. Si está apagado, la app muestra los últimos datos guardados y el aviso "Sin conexión con AWS".
- Los recursos y la URL se mantienen entre sesiones del laboratorio.
- Si el equipo prefiere un solo API Gateway, estas 4 rutas se pueden agregar al API existente, apuntando a esta Lambda.

## Pruebas locales

```bash
pip install "moto[dynamodb]" boto3 pytest
pytest -q aws/lambda_tarjetas
```

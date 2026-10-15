"""
MetroGo - Lambda de HU-004 y HU-006.

HU-004: consultar el saldo y estado de la tarjeta, configurar la alerta de saldo bajo
        y registrar movimientos (recargas y viajes) que cambian el saldo.
HU-006: listar los puntos de recarga física (estaciones) que la app muestra en el mapa.

Rutas (API Gateway HTTP API o REST API con integración proxy):
    GET  /tarjetas/{numeroTarjeta}               -> saldo, estado y configuración de alertas
    PUT  /tarjetas/{numeroTarjeta}/alertas       -> guarda umbral y si las alertas están activas
    POST /tarjetas/{numeroTarjeta}/movimientos   -> {"tipo": "RECARGA" | "VIAJE", "monto": "3.20"}
    GET  /puntos-recarga                         -> lista de estaciones con recarga

Tablas DynamoDB (nombres configurables con variables de entorno):
    TABLA_TARJETAS        (por defecto "Tarjetas")        clave de partición: numeroTarjeta (String)
    TABLA_PUNTOS_RECARGA  (por defecto "PuntosRecarga")   clave de partición: id (String)

Los montos se guardan en céntimos (enteros) para evitar errores de redondeo.
Respuesta con el mismo formato que las Lambdas de login y registro:
    {"success": true|false, "message": "...", "data": {...}}
"""

import json
import os
import re
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation

import boto3
from botocore.exceptions import ClientError

TABLA_TARJETAS = os.environ.get("TABLA_TARJETAS", "Tarjetas")
TABLA_PUNTOS = os.environ.get("TABLA_PUNTOS_RECARGA", "PuntosRecarga")
# Saldo con el que se registra una tarjeta la primera vez que se consulta (demo).
SALDO_INICIAL_CENTIMOS = int(os.environ.get("SALDO_INICIAL_CENTIMOS", "1730"))
UMBRAL_POR_DEFECTO_CENTIMOS = 500
UMBRALES_PERMITIDOS = {500, 1000, 1500, 2000}
MONTO_MAXIMO_CENTIMOS = 50000  # S/ 500.00, mismo límite que la pantalla de recarga
TARIFA_GENERAL_CENTIMOS = 320
TARIFA_ESTUDIANTE_CENTIMOS = 160
ESTADOS_TARJETA = {"ACTIVA", "BLOQUEADA", "VENCIDA"}

_dynamodb = boto3.resource("dynamodb")

# Estaciones del Metropolitano usadas para cargar la tabla la primera vez.
# Estación Central está verificada; las demás son ubicaciones aproximadas.
PUNTOS_INICIALES = [
    ("central", "Estación Central", "Cercado de Lima", "-12.05773", "-77.03598"),
    ("estadio_nacional", "Estación Estadio Nacional", "La Victoria", "-12.06750", "-77.03350"),
    ("mexico", "Estación México", "La Victoria", "-12.07600", "-77.03050"),
    ("canada", "Estación Canadá", "La Victoria", "-12.08300", "-77.02900"),
    ("javier_prado", "Estación Javier Prado", "San Isidro", "-12.09050", "-77.02750"),
    ("canaval_moreyra", "Estación Canaval y Moreyra", "San Isidro", "-12.09600", "-77.02700"),
    ("aramburu", "Estación Aramburú", "Surquillo", "-12.10150", "-77.02700"),
    ("domingo_orue", "Estación Domingo Orué", "Surquillo", "-12.10700", "-77.02700"),
    ("angamos", "Estación Angamos", "Surquillo", "-12.11350", "-77.02650"),
    ("ricardo_palma", "Estación Ricardo Palma", "Miraflores", "-12.11900", "-77.02600"),
    ("benavides", "Estación Benavides", "Miraflores", "-12.12650", "-77.02500"),
    ("28_julio", "Estación 28 de Julio", "Miraflores", "-12.13200", "-77.02400"),
    ("plaza_flores", "Estación Plaza de Flores", "Barranco", "-12.14200", "-77.02150"),
]
DESCRIPCION_PUNTO = "Recarga en efectivo en boletería y máquinas de la estación."

RUTA_TARJETA = re.compile(r"/tarjetas/([^/]+)/?$")
RUTA_ALERTAS = re.compile(r"/tarjetas/([^/]+)/alertas/?$")
RUTA_MOVIMIENTOS = re.compile(r"/tarjetas/([^/]+)/movimientos/?$")
RUTA_PUNTOS = re.compile(r"/puntos-recarga/?$")
NUMERO_TARJETA_VALIDO = re.compile(r"^[0-9]{8,19}$")


# ---------------------------------------------------------------------------
# Punto de entrada
# ---------------------------------------------------------------------------
def lambda_handler(event, context):
    metodo, ruta = _metodo_y_ruta(event)
    try:
        if metodo == "GET" and RUTA_PUNTOS.search(ruta):
            return listar_puntos_recarga()

        coincidencia = RUTA_ALERTAS.search(ruta)
        if coincidencia and metodo == "PUT":
            return configurar_alertas(coincidencia.group(1), _leer_body(event))

        coincidencia = RUTA_MOVIMIENTOS.search(ruta)
        if coincidencia and metodo == "POST":
            return registrar_movimiento(coincidencia.group(1), _leer_body(event))

        coincidencia = RUTA_TARJETA.search(ruta)
        if coincidencia and metodo == "GET":
            tipo = ((event.get("queryStringParameters") or {}).get("tipo") or "Estudiante")
            return consultar_tarjeta(coincidencia.group(1), tipo)

        return _respuesta(404, False, f"Ruta no encontrada: {metodo} {ruta}")
    except SolicitudInvalida as error:
        return _respuesta(400, False, str(error))
    except ClientError as error:
        print("Error de DynamoDB:", error.response.get("Error", {}))
        return _respuesta(500, False, "No se pudo acceder a los datos. Inténtalo más tarde.")


# ---------------------------------------------------------------------------
# HU-004: tarjeta, saldo y alertas
# ---------------------------------------------------------------------------
def consultar_tarjeta(numero, tipo="Estudiante"):
    """La primera consulta registra la tarjeta con el saldo inicial (tipo solo se usa al crearla)."""
    numero = _validar_numero(numero)
    tipo = "General" if str(tipo).strip().lower() == "general" else "Estudiante"
    tabla = _dynamodb.Table(TABLA_TARJETAS)
    item = tabla.get_item(Key={"numeroTarjeta": numero}).get("Item")
    if item is None:
        item = _registrar_tarjeta_nueva(tabla, numero, tipo)
    return _respuesta(200, True, "Saldo consultado", _tarjeta_a_json(item))


def configurar_alertas(numero, body):
    numero = _validar_numero(numero)
    umbral = _entero(body.get("umbralSaldoBajoCentimos"), "umbralSaldoBajoCentimos")
    if umbral not in UMBRALES_PERMITIDOS:
        raise SolicitudInvalida("El umbral debe ser S/ 5, 10, 15 o 20.")
    activas = body.get("alertasActivas")
    if not isinstance(activas, bool):
        raise SolicitudInvalida("alertasActivas debe ser true o false.")

    tabla = _dynamodb.Table(TABLA_TARJETAS)
    _asegurar_tarjeta(tabla, numero)
    item = tabla.update_item(
        Key={"numeroTarjeta": numero},
        UpdateExpression="SET umbralSaldoBajoCentimos = :u, alertasActivas = :a, actualizado = :t",
        ExpressionAttributeValues={":u": umbral, ":a": activas, ":t": _ahora()},
        ReturnValues="ALL_NEW",
    )["Attributes"]
    return _respuesta(200, True, "Alertas actualizadas", _tarjeta_a_json(item))


def registrar_movimiento(numero, body):
    numero = _validar_numero(numero)
    tipo = str(body.get("tipo", "")).upper()
    if tipo not in {"RECARGA", "VIAJE"}:
        raise SolicitudInvalida("tipo debe ser RECARGA o VIAJE.")

    tabla = _dynamodb.Table(TABLA_TARJETAS)
    actual = _asegurar_tarjeta(tabla, numero)

    if actual.get("estado", "ACTIVA") != "ACTIVA":
        return _respuesta(409, False, "La tarjeta no está activa.")

    if tipo == "RECARGA":
        monto = _monto_en_centimos(body.get("monto"))
        item = tabla.update_item(
            Key={"numeroTarjeta": numero},
            UpdateExpression="SET saldoCentimos = saldoCentimos + :m, actualizado = :t",
            ExpressionAttributeValues={":m": monto, ":t": _ahora()},
            ReturnValues="ALL_NEW",
        )["Attributes"]
        return _respuesta(200, True, "Recarga aplicada al saldo", _tarjeta_a_json(item))

    # VIAJE: se descuenta la tarifa según el tipo de tarjeta, solo si alcanza el saldo.
    # La condición se revisa dentro de DynamoDB, así dos viajes a la vez no dejan saldo negativo.
    tarifa = _tarifa(actual.get("tipo", "General"))
    try:
        item = tabla.update_item(
            Key={"numeroTarjeta": numero},
            UpdateExpression="SET saldoCentimos = saldoCentimos - :m, actualizado = :t",
            ConditionExpression="saldoCentimos >= :m",
            ExpressionAttributeValues={":m": tarifa, ":t": _ahora()},
            ReturnValues="ALL_NEW",
        )["Attributes"]
    except ClientError as error:
        if error.response.get("Error", {}).get("Code") == "ConditionalCheckFailedException":
            return _respuesta(409, False, "Saldo insuficiente para el viaje.")
        raise
    return _respuesta(200, True, "Viaje registrado", _tarjeta_a_json(item))


def _registrar_tarjeta_nueva(tabla, numero, tipo="Estudiante"):
    item = {
        "numeroTarjeta": numero,
        "tipo": tipo,
        "saldoCentimos": SALDO_INICIAL_CENTIMOS,
        "estado": "ACTIVA",
        "umbralSaldoBajoCentimos": UMBRAL_POR_DEFECTO_CENTIMOS,
        "alertasActivas": True,
        "actualizado": _ahora(),
    }
    try:
        # Si dos consultas llegan a la vez, solo una crea la tarjeta.
        tabla.put_item(Item=item, ConditionExpression="attribute_not_exists(numeroTarjeta)")
        return item
    except ClientError as error:
        if error.response.get("Error", {}).get("Code") == "ConditionalCheckFailedException":
            return tabla.get_item(Key={"numeroTarjeta": numero})["Item"]
        raise


def _asegurar_tarjeta(tabla, numero):
    item = tabla.get_item(Key={"numeroTarjeta": numero}).get("Item")
    return item if item is not None else _registrar_tarjeta_nueva(tabla, numero)


def _tarjeta_a_json(item):
    saldo = int(item.get("saldoCentimos", 0))
    umbral = int(item.get("umbralSaldoBajoCentimos", UMBRAL_POR_DEFECTO_CENTIMOS))
    estado = item.get("estado", "ACTIVA")
    return {
        "numeroTarjeta": item["numeroTarjeta"],
        "tipo": item.get("tipo", "General"),
        "estado": estado if estado in ESTADOS_TARJETA else "ACTIVA",
        "saldoCentimos": saldo,
        "saldo": f"{saldo / 100:.2f}",
        "umbralSaldoBajoCentimos": umbral,
        "alertasActivas": bool(item.get("alertasActivas", True)),
        "saldoBajo": saldo < umbral,
        "actualizado": item.get("actualizado"),
    }


def _tarifa(tipo):
    return TARIFA_ESTUDIANTE_CENTIMOS if str(tipo).lower() == "estudiante" else TARIFA_GENERAL_CENTIMOS


# ---------------------------------------------------------------------------
# HU-006: puntos de recarga
# ---------------------------------------------------------------------------
def listar_puntos_recarga():
    tabla = _dynamodb.Table(TABLA_PUNTOS)
    items = _escanear_todo(tabla)
    if not items:
        _cargar_puntos_iniciales(tabla)
        items = _escanear_todo(tabla)
    puntos = sorted(
        (
            {
                "id": it["id"],
                "nombre": it.get("nombre", ""),
                "distrito": it.get("distrito", ""),
                "descripcion": it.get("descripcion", DESCRIPCION_PUNTO),
                "latitud": float(it["latitud"]),
                "longitud": float(it["longitud"]),
            }
            for it in items
            if it.get("activo", True) and "latitud" in it and "longitud" in it
        ),
        key=lambda p: p["latitud"],
        reverse=True,  # de norte a sur, igual que la lista de la app
    )
    return _respuesta(200, True, "Puntos de recarga", puntos)


def _cargar_puntos_iniciales(tabla):
    with tabla.batch_writer() as lote:
        for id_, nombre, distrito, lat, lng in PUNTOS_INICIALES:
            lote.put_item(Item={
                "id": id_,
                "nombre": nombre,
                "distrito": distrito,
                "descripcion": DESCRIPCION_PUNTO,
                "latitud": Decimal(lat),
                "longitud": Decimal(lng),
                "activo": True,
            })


def _escanear_todo(tabla):
    respuesta = tabla.scan()
    items = respuesta.get("Items", [])
    while "LastEvaluatedKey" in respuesta:
        respuesta = tabla.scan(ExclusiveStartKey=respuesta["LastEvaluatedKey"])
        items.extend(respuesta.get("Items", []))
    return items


# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------
class SolicitudInvalida(Exception):
    pass


def _metodo_y_ruta(event):
    """Funciona con HTTP API (payload 2.0) y con REST API (payload 1.0)."""
    http = (event.get("requestContext") or {}).get("http") or {}
    metodo = (http.get("method") or event.get("httpMethod") or "").upper()
    ruta = event.get("rawPath") or event.get("path") or ""
    return metodo, ruta


def _leer_body(event):
    body = event.get("body") or "{}"
    if event.get("isBase64Encoded"):
        import base64
        body = base64.b64decode(body).decode("utf-8")
    try:
        datos = json.loads(body)
    except json.JSONDecodeError:
        raise SolicitudInvalida("El cuerpo debe ser JSON válido.")
    if not isinstance(datos, dict):
        raise SolicitudInvalida("El cuerpo debe ser un objeto JSON.")
    return datos


def _validar_numero(numero):
    numero = str(numero).strip()
    if not NUMERO_TARJETA_VALIDO.match(numero):
        raise SolicitudInvalida("El número de tarjeta debe tener entre 8 y 19 dígitos.")
    return numero


def _monto_en_centimos(valor):
    try:
        monto = (Decimal(str(valor)) * 100).quantize(Decimal("1"))
    except (InvalidOperation, ValueError):
        raise SolicitudInvalida("monto debe ser un número, por ejemplo \"20.00\".")
    if monto <= 0 or monto > MONTO_MAXIMO_CENTIMOS:
        raise SolicitudInvalida("El monto debe ser mayor a 0 y como máximo S/ 500.")
    return int(monto)


def _entero(valor, nombre):
    if isinstance(valor, bool):
        raise SolicitudInvalida(f"{nombre} debe ser un número entero.")
    try:
        return int(valor)
    except (TypeError, ValueError):
        raise SolicitudInvalida(f"{nombre} debe ser un número entero.")


def _ahora():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def _json_default(valor):
    if isinstance(valor, Decimal):
        return int(valor) if valor == valor.to_integral_value() else float(valor)
    raise TypeError(f"No serializable: {type(valor)}")


def _respuesta(status, success, message, data=None):
    return {
        "statusCode": status,
        "headers": {"Content-Type": "application/json; charset=utf-8"},
        "body": json.dumps(
            {"success": success, "message": message, "data": data},
            ensure_ascii=False,
            default=_json_default,
        ),
    }

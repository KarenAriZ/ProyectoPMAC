"""MetroGo: recargas simuladas por usuario. HTTP API, payload 2.0.

Compatible con el token opaco del login. Registro y saldo simulado se
confirman juntos. El UUID por usuario evita duplicar una misma operación.
No consulta ni modifica el sistema real del Metropolitano.
"""
import base64
import hashlib
import json
import logging
import os
import re
import time
from datetime import datetime, timezone
from decimal import Decimal

import boto3
from botocore.exceptions import ClientError
from boto3.dynamodb.types import TypeSerializer
import uuid

logger = logging.getLogger()
logger.setLevel(logging.INFO)
db = boto3.resource("dynamodb")
sessions = db.Table(os.environ.get("SESSIONS_TABLE_NAME", "sesiones_metrogo"))
users = db.Table(os.environ.get("USERS_TABLE_NAME", "usuario_metrogo"))
cards = db.Table(os.environ.get("CARDS_TABLE_NAME", "tarjetas_metrogo"))
links = db.Table(os.environ.get("USER_CARDS_TABLE_NAME", "usuario_tarjeta"))
NUMBER = re.compile(r"[0-9]{8,16}")


class ApiError(Exception):
    def __init__(self, status, message):
        self.status = status
        self.message = message


def timestamp():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def decimal_json(value):
    if isinstance(value, Decimal):
        return int(value) if value % 1 == 0 else float(value)
    raise TypeError("Tipo no serializable")


def response(status, message, data=None):
    body = {"success": 200 <= status < 300, "message": message, "timestamp": timestamp()}
    if data is not None:
        body["data"] = data
    return {
        "statusCode": status,
        "headers": {"Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store"},
        "body": json.dumps(body, ensure_ascii=False, default=decimal_json)
    }


def authenticate(event):
    headers = {str(k).lower(): v for k, v in (event.get("headers") or {}).items()}
    authorization = headers.get("authorization", "")
    if not isinstance(authorization, str):
        raise ApiError(401, "Inicia sesión nuevamente")
    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer" or not 20 <= len(parts[1]) <= 512:
        raise ApiError(401, "Inicia sesión nuevamente")
    token_hash = hashlib.sha256(parts[1].encode("utf-8")).hexdigest()
    session = sessions.get_item(Key={"id": token_hash}, ConsistentRead=True).get("Item")
    if not session or not session.get("usuarioId"):
        raise ApiError(401, "La sesión no es válida")
    try:
        expired = int(session.get("expiresAt", 0)) <= int(time.time())
    except (ValueError, TypeError):
        expired = True
    if expired:
        raise ApiError(401, "Tu sesión venció. Inicia sesión nuevamente")
    user_id = session["usuarioId"]
    if not users.get_item(Key={"id": user_id}, ConsistentRead=True).get("Item"):
        raise ApiError(401, "La sesión no es válida")
    return user_id


def read_body(event):
    try:
        raw = event.get("body") or "{}"
        if event.get("isBase64Encoded"):
            raw = base64.b64decode(raw, validate=True).decode("utf-8")
        if len(raw.encode("utf-8")) > 8192:
            raise ApiError(413, "Solicitud demasiado grande")
        body = json.loads(raw)
        if not isinstance(body, dict):
            raise ValueError()
        return body
    except (ValueError, TypeError, UnicodeError):
        raise ApiError(400, "El cuerpo debe ser un objeto JSON válido")


def valid_number(value):
    if not isinstance(value, str) or not NUMBER.fullmatch(value.strip()):
        raise ApiError(400, "El número de tarjeta debe tener entre 8 y 16 dígitos")
    return value.strip()


# Cliente independiente: recibe AttributeValue serializados, sin el transformador del recurso.
client = boto3.client("dynamodb")
recharges = db.Table(os.environ.get("RECHARGES_TABLE_NAME", "recargas_metrogo"))
serializer = TypeSerializer()
METHODS = ("Yape", "Plin", "Tarjeta Débito / Crédito Visa")


def av(mapping):
    return {k: serializer.serialize(v) for k, v in mapping.items()}


def valid_id(value):
    try:
        if not isinstance(value, str) or str(uuid.UUID(value)) != value:
            raise ValueError()
    except (ValueError, AttributeError):
        raise ApiError(400, "idOperacion debe ser un UUID válido")
    return value


def public(item):
    return {k: v for k, v in item.items() if k != "usuarioId"}


def existing(user_id, operation_id, body):
    item = recharges.get_item(Key={"usuarioId": user_id, "id": operation_id}, ConsistentRead=True).get("Item")
    if item:
        if any(item.get(k) != body[k] for k in ("numeroTarjeta", "montoCentimos", "metodoPago")):
            raise ApiError(409, "Este código de operación ya fue utilizado con otros datos")
        return response(200, "La recarga simulada ya estaba registrada", public(item))


def create_recharge(user_id, body):
    allowed = {"idOperacion", "numeroTarjeta", "montoCentimos", "metodoPago"}
    if set(body) != allowed:
        raise ApiError(400, "Envía únicamente idOperacion, numeroTarjeta, montoCentimos y metodoPago")
    operation_id = valid_id(body["idOperacion"])
    number = valid_number(body["numeroTarjeta"])
    # bool es un subtipo de int en Python: no aceptarlo como importe.
    amount = body["montoCentimos"]
    if type(amount) is not int or not 1 <= amount <= 50000:
        raise ApiError(400, "El monto debe ser de S/ 0.01 a S/ 500.00, expresado en céntimos enteros")
    method = body["metodoPago"]
    if method not in METHODS:
        raise ApiError(400, "Método de pago no válido")
    for attempt in range(3):
        prior = existing(user_id, operation_id, body)
        if prior:
            return prior
        link = links.get_item(Key={"usuarioId": user_id, "numeroTarjeta": number}, ConsistentRead=True).get("Item")
        if not link:
            raise ApiError(404, "No se encontró esa tarjeta en tu cuenta")
        if link.get("estado") != "ACTIVA":
            raise ApiError(409, "Reactiva la tarjeta antes de recargar")
        card = cards.get_item(Key={"numeroTarjeta": number}, ConsistentRead=True).get("Item")
        if not card or card.get("estado") != "ACTIVA" or card.get("saldoSimulado") is not True:
            raise ApiError(409, "La tarjeta no está activa para recargas simuladas")
        balance = card.get("saldoCentimos")
        if not isinstance(balance, (int, Decimal)) or isinstance(balance, bool) or balance < 0 or balance % 1:
            raise ApiError(409, "La tarjeta no tiene un saldo simulado válido")
        now = timestamp()
        item = {
            "usuarioId": user_id, "id": operation_id, "numeroTarjeta": number,
            "aliasTarjeta": link.get("alias", "Mi tarjeta"), "tipoTarjeta": card.get("tipo", "General"),
            "montoCentimos": amount, "metodoPago": method, "fecha": now,
            "estado": "SIMULADA", "simulada": True,
            "saldoAnteriorCentimos": int(balance), "saldoPosteriorCentimos": int(balance) + amount
        }
        try:
            client.transact_write_items(TransactItems=[
                {"Put": {"TableName": recharges.name, "Item": av(item),
                         "ConditionExpression": "attribute_not_exists(usuarioId)"}},
                {"Update": {"TableName": cards.name, "Key": av({"numeroTarjeta": number}),
                            "UpdateExpression": "SET saldoCentimos = :new, fechaActualizacion = :now",
                            "ConditionExpression": "saldoCentimos = :old AND saldoSimulado = :yes AND #state = :active",
                            "ExpressionAttributeNames": {"#state": "estado"},
                            "ExpressionAttributeValues": av({":new": int(balance) + amount, ":old": int(balance),
                                                             ":yes": True, ":active": "ACTIVA", ":now": now})}},
                {"ConditionCheck": {"TableName": links.name,
                                    "Key": av({"usuarioId": user_id, "numeroTarjeta": number}),
                                    "ConditionExpression": "#state = :active",
                                    "ExpressionAttributeNames": {"#state": "estado"},
                                    "ExpressionAttributeValues": av({":active": "ACTIVA"})}}
            ])
            return response(201, "Recarga simulada registrada", public(item))
        except ClientError as error:
            if error.response.get("Error", {}).get("Code") != "TransactionCanceledException":
                raise
            reasons = error.response.get("CancellationReasons", [])
            if any(r.get("Code") not in (None, "None", "ConditionalCheckFailed", "TransactionConflict") for r in reasons):
                raise
            # Releer: otro intento pudo completar la misma operación o modificar el saldo.
    prior = existing(user_id, operation_id, body)
    if prior:
        return prior
    raise ApiError(409, "La tarjeta cambió durante la operación. Reintenta con el mismo código")


def list_recharges(user_id, query):
    args = {"KeyConditionExpression": "usuarioId = :u", "ExpressionAttributeValues": {":u": user_id},
            "ConsistentRead": True, "Limit": 50}
    if query.get("nextKey"):
        try:
            cursor = query["nextKey"]
            if not isinstance(cursor, str) or len(cursor) > 4096:
                raise ValueError()
            key = json.loads(base64.urlsafe_b64decode(cursor).decode("utf-8"))
            if not isinstance(key, dict) or key.get("usuarioId") != user_id:
                raise ValueError()
            args["ExclusiveStartKey"] = {"usuarioId": user_id, "id": valid_id(key.get("id"))}
        except (ValueError, TypeError, UnicodeError):
            raise ApiError(400, "Cursor no válido")
    result = recharges.query(**args)
    key = result.get("LastEvaluatedKey")
    cursor = base64.urlsafe_b64encode(json.dumps(key).encode()).decode() if key else None
    return response(200, "Recargas consultadas", {"items": [public(i) for i in result.get("Items", [])], "nextKey": cursor})


def lambda_handler(event, context):
    try:
        route = event.get("routeKey")
        if route not in ("POST /recargas", "GET /recargas", "GET /recargas/{id}"):
            raise ApiError(404, "Ruta no encontrada")
        if event.get("requestContext", {}).get("http", {}).get("method") != route.split()[0]:
            raise ApiError(405, "Método no permitido")
        user_id = authenticate(event)
        if route == "POST /recargas":
            return create_recharge(user_id, read_body(event))
        if route == "GET /recargas":
            return list_recharges(user_id, event.get("queryStringParameters") or {})
        operation_id = valid_id((event.get("pathParameters") or {}).get("id"))
        item = recharges.get_item(Key={"usuarioId": user_id, "id": operation_id}, ConsistentRead=True).get("Item")
        if not item:
            raise ApiError(404, "No se encontró la recarga en tu cuenta")
        return response(200, "Recarga consultada", public(item))
    except ApiError as error:
        return response(error.status, error.message)
    except ClientError as error:
        logger.error("DynamoDB recargas: %s", error.response.get("Error", {}).get("Code"))
        return response(500, "No se pudo confirmar la recarga. Reintenta con el mismo código de operación")
    except Exception:
        logger.exception("Error interno en recargas")
        return response(500, "No se pudo confirmar la recarga. Reintenta con el mismo código de operación")

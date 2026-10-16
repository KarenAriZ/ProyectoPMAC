"""Pruebas locales; moto intercepta todas las llamadas AWS."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import time
import uuid

import boto3
import pytest
from moto import mock_aws

NUMBER = "8754785478"
TOKEN = "token-de-prueba-largo-usuario-A"
OTHER = "token-de-prueba-largo-usuario-B"


@pytest.fixture
def fn(monkeypatch):
    for key, value in {"AWS_DEFAULT_REGION": "us-east-1", "AWS_ACCESS_KEY_ID": "test", "AWS_SECRET_ACCESS_KEY": "test"}.items():
        monkeypatch.setenv(key, value)
    with mock_aws():
        db = boto3.resource("dynamodb", region_name="us-east-1")
        for name, keys in [("sesiones_metrogo", ["id"]), ("usuario_metrogo", ["id"]),
                           ("tarjetas_metrogo", ["numeroTarjeta"]),
                           ("usuario_tarjeta", ["usuarioId", "numeroTarjeta"]),
                           ("recargas_metrogo", ["usuarioId", "id"])]:
            db.create_table(TableName=name, BillingMode="PAY_PER_REQUEST",
                            KeySchema=[{"AttributeName": k, "KeyType": "HASH" if i == 0 else "RANGE"} for i, k in enumerate(keys)],
                            AttributeDefinitions=[{"AttributeName": k, "AttributeType": "S"} for k in keys])
        spec = importlib.util.spec_from_file_location("recargas_lambda_test", Path(__file__).with_name("lambda_function.py"))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        for token, user in [(TOKEN, "A"), (OTHER, "B")]:
            module.users.put_item(Item={"id": user})
            module.sessions.put_item(Item={"id": hashlib.sha256(token.encode()).hexdigest(), "usuarioId": user,
                                           "expiresAt": int(time.time()) + 3600})
        module.cards.put_item(Item={"numeroTarjeta": NUMBER, "tipo": "General", "estado": "ACTIVA", "saldoSimulado": True, "saldoCentimos": 0})
        module.links.put_item(Item={"usuarioId": "A", "numeroTarjeta": NUMBER, "alias": "Trabajo", "estado": "ACTIVA"})
        yield module


def body():
    return {"idOperacion": str(uuid.uuid4()), "numeroTarjeta": NUMBER, "montoCentimos": 2050, "metodoPago": "Yape"}


def call(fn, route="POST /recargas", data=None, token=TOKEN, query=None, path=None):
    event = {"routeKey": route, "requestContext": {"http": {"method": route.split()[0]}},
             "headers": {"Authorization": "Bearer " + token}, "queryStringParameters": query, "pathParameters": path}
    if data is not None:
        event["body"] = json.dumps(data)
    result = fn.lambda_handler(event, None)
    return result["statusCode"], json.loads(result["body"])


def balance(fn):
    return fn.cards.get_item(Key={"numeroTarjeta": NUMBER}, ConsistentRead=True)["Item"]["saldoCentimos"]


def test_registro_y_saldo_confirmados_juntos(fn):
    request = body()
    code, result = call(fn, data=request)
    assert code == 201 and balance(fn) == 2050
    assert result["data"]["saldoAnteriorCentimos"] == 0
    assert result["data"]["saldoPosteriorCentimos"] == 2050
    assert result["data"]["simulada"] is True
    assert "usuarioId" not in result["data"]
    assert fn.recharges.get_item(Key={"usuarioId": "A", "id": request["idOperacion"]})["Item"]["metodoPago"] == "Yape"


def test_reintento_no_duplica_incluso_si_se_desactiva_despues(fn):
    request = body()
    assert call(fn, data=request)[0] == 201
    fn.links.update_item(Key={"usuarioId": "A", "numeroTarjeta": NUMBER}, UpdateExpression="SET #s = :s",
                         ExpressionAttributeNames={"#s": "estado"}, ExpressionAttributeValues={":s": "INACTIVA"})
    assert call(fn, data=request)[0] == 200
    assert balance(fn) == 2050
    assert len(fn.recharges.scan()["Items"]) == 1


def test_codigo_reutilizado_con_otros_datos_rechazado(fn):
    request = body()
    call(fn, data=request)
    request["montoCentimos"] = 500
    assert call(fn, data=request)[0] == 409
    assert balance(fn) == 2050


@pytest.mark.parametrize("amount", [0, -1, 50001, True, 1.5, "2050"])
def test_montos_invalidos_no_escriben(fn, amount):
    request = body(); request["montoCentimos"] = amount
    assert call(fn, data=request)[0] == 400
    assert balance(fn) == 0 and fn.recharges.scan()["Items"] == []


@pytest.mark.parametrize("extra", ["usuarioId", "saldoCentimos", "estado", "fecha"])
def test_campos_controlados_por_servidor_no_aceptados(fn, extra):
    request = body(); request[extra] = "B"
    assert call(fn, data=request)[0] == 400
    assert balance(fn) == 0


def test_cuentas_aisladas(fn):
    request = body()
    assert call(fn, data=request, token=OTHER)[0] == 404
    assert call(fn, data=request)[0] == 201
    code, result = call(fn, "GET /recargas", token=OTHER)
    assert code == 200 and result["data"]["items"] == []
    assert call(fn, "GET /recargas/{id}", token=OTHER, path={"id": request["idOperacion"]})[0] == 404


def test_sesion_vencida(fn):
    fn.sessions.update_item(Key={"id": hashlib.sha256(TOKEN.encode()).hexdigest()}, UpdateExpression="SET expiresAt = :t",
                            ExpressionAttributeValues={":t": 1})
    assert call(fn, data=body())[0] == 401
    assert balance(fn) == 0


def test_transaccion_cancelada_no_deja_registro_parcial(fn, monkeypatch):
    real = fn.client.transact_write_items
    def inactive(**kwargs):
        fn.links.update_item(Key={"usuarioId": "A", "numeroTarjeta": NUMBER}, UpdateExpression="SET #s = :s",
                             ExpressionAttributeNames={"#s": "estado"}, ExpressionAttributeValues={":s": "INACTIVA"})
        return real(**kwargs)
    monkeypatch.setattr(fn.client, "transact_write_items", inactive)
    assert call(fn, data=body())[0] == 409
    assert balance(fn) == 0 and fn.recharges.scan()["Items"] == []


def test_conflicto_saldo_relee_y_no_pierde_incremento(fn, monkeypatch):
    real = fn.client.transact_write_items
    calls = []
    def conflict(**kwargs):
        if not calls:
            fn.cards.update_item(Key={"numeroTarjeta": NUMBER}, UpdateExpression="SET saldoCentimos = :b", ExpressionAttributeValues={":b": 100})
        calls.append(1)
        return real(**kwargs)
    monkeypatch.setattr(fn.client, "transact_write_items", conflict)
    code, result = call(fn, data=body())
    assert code == 201 and balance(fn) == 2150
    assert result["data"]["saldoAnteriorCentimos"] == 100 and len(calls) == 2


def test_cursor_no_puede_consultar_otro_usuario(fn):
    import base64
    cursor = base64.urlsafe_b64encode(json.dumps({"usuarioId": "B", "id": str(uuid.uuid4())}).encode()).decode()
    assert call(fn, "GET /recargas", query={"nextKey": cursor})[0] == 400


def test_historial_paginado(fn):
    template = {"usuarioId": "A", "numeroTarjeta": NUMBER, "montoCentimos": 10}
    for _ in range(51):
        fn.recharges.put_item(Item={**template, "id": str(uuid.uuid4())})
    _, first = call(fn, "GET /recargas")
    assert len(first["data"]["items"]) == 50
    _, second = call(fn, "GET /recargas", query={"nextKey": first["data"]["nextKey"]})
    assert len(second["data"]["items"]) == 1 and second["data"]["nextKey"] is None

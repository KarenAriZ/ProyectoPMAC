"""
Pruebas del Lambda con una DynamoDB simulada (moto). No toca AWS real.
Ejecutar:  pip install "moto[dynamodb]" boto3 pytest  &&  pytest -q
"""
import json
import os
import sys

import boto3
import pytest
from moto import mock_aws

os.environ.setdefault("AWS_DEFAULT_REGION", "us-east-1")
os.environ.setdefault("AWS_ACCESS_KEY_ID", "test")
os.environ.setdefault("AWS_SECRET_ACCESS_KEY", "test")
sys.path.insert(0, os.path.dirname(__file__))

TARJETA = "8754785478"


@pytest.fixture
def lambda_fn():
    with mock_aws():
        db = boto3.client("dynamodb", region_name="us-east-1")
        for tabla, clave in (("Tarjetas", "numeroTarjeta"), ("PuntosRecarga", "id")):
            db.create_table(
                TableName=tabla,
                KeySchema=[{"AttributeName": clave, "KeyType": "HASH"}],
                AttributeDefinitions=[{"AttributeName": clave, "AttributeType": "S"}],
                BillingMode="PAY_PER_REQUEST",
            )
        import importlib
        import lambda_function
        importlib.reload(lambda_function)  # crea el recurso dentro del mock
        yield lambda_function


def llamar(fn, metodo, ruta, body=None, query=None, rest_api=False):
    if rest_api:  # formato REST API (payload 1.0)
        event = {"httpMethod": metodo, "path": ruta}
    else:         # formato HTTP API (payload 2.0)
        event = {"rawPath": ruta, "requestContext": {"http": {"method": metodo}}}
    if body is not None:
        event["body"] = json.dumps(body)
    if query:
        event["queryStringParameters"] = query
    r = fn.lambda_handler(event, None)
    return r["statusCode"], json.loads(r["body"])


def test_primera_consulta_registra_tarjeta_con_saldo_inicial(lambda_fn):
    status, body = llamar(lambda_fn, "GET", f"/tarjetas/{TARJETA}")
    assert status == 200 and body["success"] is True
    data = body["data"]
    assert data["saldoCentimos"] == 1730 and data["saldo"] == "17.30"
    assert data["estado"] == "ACTIVA" and data["tipo"] == "Estudiante"
    assert data["umbralSaldoBajoCentimos"] == 500 and data["alertasActivas"] is True
    assert data["saldoBajo"] is False


def test_tipo_general_al_crear_y_formato_rest_api(lambda_fn):
    status, body = llamar(lambda_fn, "GET", "/tarjetas/1234567890", query={"tipo": "General"}, rest_api=True)
    assert status == 200 and body["data"]["tipo"] == "General"


def test_configurar_alertas_y_saldo_bajo(lambda_fn):
    status, body = llamar(lambda_fn, "PUT", f"/tarjetas/{TARJETA}/alertas",
                          {"umbralSaldoBajoCentimos": 2000, "alertasActivas": True})
    assert status == 200
    assert body["data"]["umbralSaldoBajoCentimos"] == 2000 and body["data"]["saldoBajo"] is True
    status, body = llamar(lambda_fn, "PUT", f"/tarjetas/{TARJETA}/alertas",
                          {"umbralSaldoBajoCentimos": 700, "alertasActivas": True})
    assert status == 400
    status, _ = llamar(lambda_fn, "PUT", f"/tarjetas/{TARJETA}/alertas",
                       {"umbralSaldoBajoCentimos": 500, "alertasActivas": "si"})
    assert status == 400


def test_recarga_suma_y_viaje_descuenta_tarifa(lambda_fn):
    status, body = llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "RECARGA", "monto": "20.00"})
    assert status == 200 and body["data"]["saldoCentimos"] == 3730
    status, body = llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "VIAJE"})
    assert status == 200 and body["data"]["saldoCentimos"] == 3570  # estudiante: S/ 1.60


def test_viaje_sin_saldo_suficiente_no_deja_negativo(lambda_fn):
    for _ in range(10):  # 17.30 / 1.60 = 10 viajes
        status, body = llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "VIAJE"})
        assert status == 200
    assert body["data"]["saldoCentimos"] == 130
    status, body = llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "VIAJE"})
    assert status == 409 and body["success"] is False


def test_validaciones(lambda_fn):
    assert llamar(lambda_fn, "GET", "/tarjetas/abc")[0] == 400
    assert llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "RECARGA", "monto": "-5"})[0] == 400
    assert llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "RECARGA", "monto": "900"})[0] == 400
    assert llamar(lambda_fn, "POST", f"/tarjetas/{TARJETA}/movimientos", {"tipo": "OTRO", "monto": "5"})[0] == 400
    assert llamar(lambda_fn, "DELETE", f"/tarjetas/{TARJETA}")[0] == 404
    event = {"rawPath": f"/tarjetas/{TARJETA}/movimientos", "requestContext": {"http": {"method": "POST"}}, "body": "no-json"}
    assert lambda_fn.lambda_handler(event, None)["statusCode"] == 400


def test_puntos_recarga_se_cargan_la_primera_vez(lambda_fn):
    status, body = llamar(lambda_fn, "GET", "/puntos-recarga")
    assert status == 200 and len(body["data"]) == 13
    central = next(p for p in body["data"] if p["id"] == "central")
    assert central["latitud"] == -12.05773 and central["longitud"] == -77.03598
    # segunda llamada: no duplica
    assert len(llamar(lambda_fn, "GET", "/puntos-recarga")[1]["data"]) == 13


def test_ruta_con_prefijo_de_stage(lambda_fn):
    status, _ = llamar(lambda_fn, "GET", f"/prod/tarjetas/{TARJETA}")
    assert status == 200

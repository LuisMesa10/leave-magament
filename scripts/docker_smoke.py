"""Prueba reproducible del contenedor y de la persistencia local; solo datos sinteticos."""
import base64
import json
import os
import sys
import time
from datetime import date, timedelta
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import URLError

BASE = "http://127.0.0.1:8080/api/leaves"

def call(path="", method="GET", data=None, role="employee"):
    password = os.environ["LEAVE_" + role.upper() + "_PASSWORD"]
    headers = {
        "Authorization": "Basic " + base64.b64encode((role + ":" + password).encode()).decode(),
        "X-Requested-With": "leave-api",
        "Content-Type": "application/json",
    }
    payload = None if data is None else json.dumps(data).encode()
    with urlopen(Request(BASE + path, data=payload, headers=headers, method=method), timeout=5) as response:
        return response.status, json.load(response)

def ready():
    for attempt in range(30):
        try:
            call("/summary", role="manager")
            return
        except (URLError, TimeoutError):
            time.sleep(2)
    raise RuntimeError("El contenedor no quedo disponible en 60 segundos")

ready()
if sys.argv[1] == "seed":
    start = date.today() + timedelta(days=10)
    status, result = call(method="POST", data={
        "employeeName": "Docker smoke test",
        "type": "PERSONAL",
        "startDate": start.isoformat(),
        "endDate": (start + timedelta(days=1)).isoformat(),
        "reason": "Datos sinteticos",
    })
    assert status == 201 and result["status"] == "PENDING", result
    Path("smoke-request-id.txt").write_text(str(result["id"]))
elif sys.argv[1] == "verify":
    request_id = Path("smoke-request-id.txt").read_text()
    _, result = call("/" + request_id)
    assert result["employeeName"] == "Docker smoke test", result
    _, result = call("/" + request_id + "/approve", "POST",
                     {"comment": "Persistencia verificada"}, "manager")
    assert result["status"] == "APPROVED", result
    _, history = call("/" + request_id + "/history", role="manager")
    assert [event["action"] for event in history["content"]] == ["CREATED", "APPROVED"], history
else:
    raise ValueError("Use seed o verify")
print("Docker smoke:", sys.argv[1], "OK")

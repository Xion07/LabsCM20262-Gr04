"""Genera src/main/resources/seed/clients.json: 20 clientes FICTICIOS para la demo.

Los nombres, cedulas y placas son inventados. Las fechas se guardan como desfases respecto a
"hoy" (dueInDays, daysAgo...) para que la demo siempre muestre mora, vencimientos cercanos, etc.
Es determinista: correrlo de nuevo produce el mismo archivo.

    python tools/generate_seed.py
"""
import json
import random
from pathlib import Path

rng = random.Random(2026)

FIRST = ["Camilo", "Andrés", "Luis", "Jorge", "Yeison", "Kevin", "Brayan", "Duván", "Edwin", "Fabián",
         "Wilmer", "Jhon", "Héctor", "Ricardo", "Sebastián", "Óscar", "Darwin", "Cristian", "Álvaro", "Mauricio"]
LAST = ["Restrepo", "Pérez", "Gómez", "Herrera", "Martínez", "Torres", "Ruiz", "Castro", "Mendoza", "Ortega",
        "Álvarez", "Díaz", "Vargas", "Rojas", "Cabrera", "Jiménez", "Salcedo", "Meza", "Barrios", "Polo"]
LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ"

# (dueInDays, descripcion). Negativo = dias de mora. Cuotas semanales: el desfase va de -6 a 7.
DUE = [5, -3, 0, 1, 2, -10, -1, 3, 7, 4, 6, -6, 2, 5, 1, 7, 3, -2, 4, 6]
NO_FINES = {4, 9, 14}       # clientes sin multas (estado vacio)
NO_PAYMENTS = {17}          # cliente nuevo sin abonos (estado vacio)


def plate():
    return "".join(rng.choice(LETTERS) for _ in range(3)) + f"{rng.randint(10, 99)}" + rng.choice(LETTERS)


def history(km_total):
    """Cuatro lecturas (hoy, -10, -20, -30 dias) que terminan exactamente en km_total."""
    readings = []
    km = km_total
    for ago in (0, 10, 20, 30):
        readings.append({"daysAgo": ago, "km": km})
        km = max(0, km - int(10 * rng.uniform(88, 110)))
    readings.reverse()
    return readings


clients = []
for i in range(20):
    installment = rng.choice([180_000, 210_000, 250_000, 280_000, 310_000, 350_000])
    weeks_total = rng.choice([78, 104, 130])
    weeks_paid = rng.randint(6, weeks_total - 10)
    total = installment * weeks_total
    balance = installment * (weeks_total - weeks_paid)
    days_since_start = weeks_paid * 7
    km_total = int(days_since_start * rng.uniform(88, 110))

    payments = [] if i in NO_PAYMENTS else [
        {"daysAgo": 7 * k + rng.randint(0, 2), "amount": installment} for k in range(1, 5)
    ]
    fines = [] if i in NO_FINES else [
        {"monthsAgo": rng.randint(1, 6), "amount": 25_000, "reason": "LATE_PAYMENT",
         "installment": rng.randint(3, max(4, weeks_paid))}
        for _ in range(rng.randint(1, 3))
    ]
    km_history = history(km_total)

    clients.append({
        "document": f"{1_000_000_001 + i}",
        "holder": f"{FIRST[i]} {LAST[(i * 7) % 20]}",
        "plate": plate(),
        "totalValue": total,
        "balance": balance,
        "installment": installment,
        "dueInDays": DUE[i],
        "remainingMonths": max(1, round((weeks_total - weeks_paid) / 4.3)),
        "kmTraveled": km_total,
        "payments": payments,
        "fines": fines,
        "kmHistory": km_history,
    })

# El primero es el usuario de demostracion de siempre.
clients[0].update({"holder": "Camilo Restrepo", "plate": "ABC12D", "totalValue": 6_800_000,
                   "balance": 2_450_000, "installment": 350_000, "remainingMonths": 14, "kmTraveled": 2_870,
                   "kmHistory": history(2_870)})

out = Path(__file__).resolve().parent.parent / "src/main/resources/seed/clients.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps({"clients": clients}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"{len(clients)} clientes escritos en {out}")

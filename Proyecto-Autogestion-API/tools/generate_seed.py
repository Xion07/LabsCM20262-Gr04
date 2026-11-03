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
pay_rng = random.Random(7)  # aparte, para variar abonos sin alterar el resto de los datos

FIRST = ["Camilo", "Andrés", "Luis", "Jorge", "Yeison", "Kevin", "Brayan", "Duván", "Edwin", "Fabián",
         "Wilmer", "Jhon", "Héctor", "Ricardo", "Sebastián", "Óscar", "Darwin", "Cristian", "Álvaro", "Mauricio"]
LAST = ["Restrepo", "Pérez", "Gómez", "Herrera", "Martínez", "Torres", "Ruiz", "Castro", "Mendoza", "Ortega",
        "Álvarez", "Díaz", "Vargas", "Rojas", "Cabrera", "Jiménez", "Salcedo", "Meza", "Barrios", "Polo"]
LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ"

# dueInDays: dias hasta la proxima cuota (negativo = dias de mora). Cuotas semanales.
DUE = [5, -3, 0, 1, 2, -10, -1, 3, 7, 4, 6, -6, 2, 5, 1, 7, 3, -2, 4, 6]
NO_FINES = {4, 9, 14}       # clientes sin multas (estado vacio)
NO_PAYMENTS = {17}          # cliente nuevo: aun no ha pagado ninguna cuota (estado vacio)

# Perfil de pago de cada cliente (indice 0 = cedula ...001).
#   puntual: paga cada semana, a veces antes del dia.
#   ocasional: casi siempre puntual, con algun atraso de 2 a 3 dias.
#   irregular: se salta semanas, paga parcial y luego se pone al dia.
PROFILE = {}
for i in (0, 2, 3, 7, 9, 13, 15, 18):
    PROFILE[i] = "puntual"
for i in (4, 8, 10, 14, 16, 19):
    PROFILE[i] = "ocasional"
for i in (1, 5, 6, 11, 12):
    PROFILE[i] = "irregular"
PROFILE[17] = "nuevo"

# plazo en semanas -> rango del valor total del contrato (moto + intereses), en COP
TOTALS = {52: (6_500_000, 9_000_000), 78: (8_500_000, 11_500_000), 104: (10_000_000, 12_500_000)}
MAX_HISTORY = 12                                                        # abonos mas recientes que se guardan


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


def payment_history(profile, installment, due, weeks_paid):
    """Abonos semanales recientes (mas nuevo primero) y las cuotas que llegaron tarde.

    La cuota k vencio hace (7k - due) dias. Se paga ese dia, antes o, si hay atraso, despues.
    Devuelve (abonos, cuotas_tardias) donde cada tardia es (k, numero_de_cuota).
    """
    payments, late = [], []
    carry = 0  # lo que quedo debiendo de semanas saltadas, se paga junto con la siguiente cuota
    for k in range(1, min(MAX_HISTORY, weeks_paid) + 1):
        due_ago = 7 * k - due
        amount = installment + carry
        delay = 0
        if profile == "puntual":
            delay = -pay_rng.choice([0, 0, 0, 1, 2])           # a tiempo o 1-2 dias antes
        elif profile == "ocasional":
            if pay_rng.random() < 0.15:
                delay = pay_rng.randint(2, 3)
            else:
                delay = -pay_rng.choice([0, 0, 1])
        elif profile == "irregular":
            r = pay_rng.random()
            if r < 0.2:                                        # se salta la semana
                carry += installment
                late.append((k, weeks_paid - k + 1))
                continue
            if r < 0.45:                                       # abono parcial
                amount = int(round(installment * pay_rng.choice([0.5, 0.7]), -4)) + carry
                late.append((k, weeks_paid - k + 1))
            else:
                delay = pay_rng.randint(1, 4)
                late.append((k, weeks_paid - k + 1))
        carry = 0
        if delay > 1 and profile != "irregular":
            late.append((k, weeks_paid - k + 1))
        payments.append({"daysAgo": max(1, due_ago - delay), "amount": amount})
    return payments, late


clients = []
for i in range(20):
    profile = PROFILE[i]
    weeks_total = rng.choice(list(TOTALS))
    low, high = TOTALS[weeks_total]
    installment = int(round(rng.uniform(low, high) / weeks_total, -4))   # cuota semanal, multiplo de 10.000
    weeks_paid = rng.randint(8, weeks_total - 10)
    if i == 0:                                   # usuario de demostracion de siempre
        installment, weeks_total, weeks_paid = 150_000, 78, 30
    if profile == "nuevo":
        weeks_paid = 0
    total = installment * weeks_total
    balance = installment * (weeks_total - weeks_paid)
    km_total = int(max(1, weeks_paid) * 7 * rng.uniform(88, 110))

    if profile == "nuevo":
        payments, late = [], []
    else:
        payments, late = payment_history(profile, installment, DUE[i], weeks_paid)
    fines = [] if (i in NO_FINES or profile in ("puntual", "nuevo")) else [
        {"monthsAgo": max(1, k // 4), "amount": 25_000, "reason": "LATE_PAYMENT", "installment": n}
        for k, n in late[:3]
    ]

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
        "kmHistory": history(km_total),
    })

clients[0].update({"holder": "Camilo Restrepo", "plate": "ABC12D"})

out = Path(__file__).resolve().parent.parent / "src/main/resources/seed/clients.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps({"clients": clients}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"{len(clients)} clientes escritos en {out}")
